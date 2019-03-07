package com.netflix.conductor.dao.es6rest.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.metadata.events.EventExecution;
import com.netflix.conductor.common.metadata.events.EventHandler;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.dao.MetadataDAO;
import com.netflix.conductor.dao.MetricsDAO;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.AvgAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.ParsedAvg;
import org.elasticsearch.search.aggregations.metrics.cardinality.ParsedCardinality;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author Oleksiy Lysak
 */
public class Elasticsearch6RestMetricsDAO extends Elasticsearch6RestAbstractDAO implements MetricsDAO {
	private static final Logger logger = LoggerFactory.getLogger(Elasticsearch6RestMetricsDAO.class);
	private static final List<String> WORKFLOW_TODAY_STATUSES = Arrays.asList(
		Workflow.WorkflowStatus.COMPLETED.name(),
		Workflow.WorkflowStatus.CANCELLED.name(),
		Workflow.WorkflowStatus.TIMED_OUT.name(),
		Workflow.WorkflowStatus.RUNNING.name(),
		Workflow.WorkflowStatus.FAILED.name()
	);
	private static final List<String> WORKFLOW_OVERALL_STATUSES = Arrays.asList(
		Workflow.WorkflowStatus.COMPLETED.name(),
		Workflow.WorkflowStatus.CANCELLED.name(),
		Workflow.WorkflowStatus.TIMED_OUT.name(),
		Workflow.WorkflowStatus.FAILED.name()
	);

	private static final List<String> TASK_TYPES = Arrays.asList(
		"WAIT",
		"HTTP",
		"BATCH",
		"SUB_WORKFLOW");

	private static final List<String> TASK_STATUSES = Arrays.asList(
		Task.Status.COMPLETED.name(),
		Task.Status.FAILED.name()
	);

	private static final List<String> EVENT_STATUSES = Arrays.asList(
		EventExecution.Status.COMPLETED.name(),
		EventExecution.Status.SKIPPED.name(),
		EventExecution.Status.FAILED.name()
	);

	private static final List<String> WORKFLOWS = Arrays.asList(
		"deluxe.dependencygraph.assembly.conformancegroup.process", // Sherlock V1 Assembly Conformance
		"deluxe.dependencygraph.sourcewait.process",                // Sherlock V2 Sourcewait
		"deluxe.dependencygraph.execute.process",                   // Sherlock V2 Execute
		"deluxe.deluxeone.sky.compliance.process",                  // Sky Compliance
		"deluxe.delivery.itune.process"                             // iTune
	);
	private static final AvgAggregationBuilder averageExecTime = AggregationBuilders.avg("aggAvg")
		.script(new Script("doc['endTime'].value != null && doc['endTime'].value > 0 " +
			" && doc['startTime'].value != null && doc['startTime'].value > 0 " +
			" ? doc['endTime'].value - doc['startTime'].value : 0"));

	private static final String prefix = "deluxe.conductor";
	private static final String version = "\\.\\d+\\.\\d+"; // covers '.X.Y' where X and Y any number
	private final MetadataDAO metadataDAO;
	private final String workflowIndex;
	private final String eventIndex;
	private final String taskIndex;

	@Inject
	public Elasticsearch6RestMetricsDAO(RestHighLevelClient client, Configuration config,
										ObjectMapper mapper, MetadataDAO metadataDAO) {
		super(client, config, mapper, "metrics");
		this.metadataDAO = metadataDAO;
		this.workflowIndex = String.format("conductor.runtime.%s.workflow", config.getStack());
		this.eventIndex = String.format("conductor.runtime.%s.event_execution", config.getStack());
		this.taskIndex = String.format("conductor.runtime.%s.task", config.getStack());
	}

	@Override
	public Map<String, Object> getMetrics() {
		Map<String, AtomicLong> output = new ConcurrentHashMap<>();

		// Filter definitions excluding sub workflows and maintenance workflows
		Set<String> fullNames = metadataDAO.findAll().stream()
			.filter(fullName -> WORKFLOWS.stream().anyMatch(shortName -> fullName.matches(shortName + version)))
			.collect(Collectors.toSet());

		// Using ExecutorService to process in parallel
		ExecutorService pool = Executors.newCachedThreadPool();
		try {
			List<Future<?>> futures = new LinkedList<>();

			// today + overall
			for (boolean today : Arrays.asList(true, false)) {

				// then per each short name
				for (String shortName : WORKFLOWS) {
					// Filter workflow definitions to have current short related only
					Set<String> filtered = fullNames.stream().filter(type -> type.startsWith(shortName)).collect(Collectors.toSet());

					// Workflow counter per short name
					futures.add(pool.submit(() -> workflowCounters(output, today, shortName, filtered)));

					// Workflow average per short name
					futures.add(pool.submit(() -> workflowAverage(output, today, shortName, filtered)));
				}

				// Overall workflow execution average
				futures.add(pool.submit(() -> overallWorkflowAverage(output, today, fullNames)));

				// Task type/refName/status counter
				futures.add(pool.submit(() -> taskTypeRefNameCounters(output, today)));

				// Task type/refName average
				futures.add(pool.submit(() -> taskTypeRefNameAverage(output, today)));

				// Event counters
				futures.add(pool.submit(() -> eventCounters(output, today)));
			}

			// Wait until completed
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (Exception ex) {
					logger.error("Get future failed " + ex.getMessage(), ex);
				}
			}
		} finally {
			pool.shutdown();
		}

		return new HashMap<>(output);
	}

	private void eventCounters(Map<String, AtomicLong> map, boolean today) {
		// 0 - event bus, 1 - subject, 2 - queue
		List<String> subjects = metadataDAO.getEventHandlers().stream()
			.filter(EventHandler::isActive)
			.map(eh -> eh.getEvent().split(":")[1])
			.collect(Collectors.toList());

		for (String subject : subjects) {
			initMetric(map, String.format("%s.event_total%s.%s", prefix, toLabel(today), subject));

			for (String status : EVENT_STATUSES) {
				initMetric(map, String.format("%s.event_%s%s.%s", prefix, status.toLowerCase(), toLabel(today), subject));
			}
		}

		QueryBuilder typeQuery = QueryBuilders.termsQuery("subject.keyword", subjects);
		QueryBuilder statusQuery = QueryBuilders.termsQuery("status", EVENT_STATUSES);
		BoolQueryBuilder mainQuery = QueryBuilders.boolQuery().must(typeQuery).must(statusQuery);
		if (today) {
			mainQuery = mainQuery.must(getStartTimeQuery("created"));
		}

		TermsAggregationBuilder aggregation = AggregationBuilders
			.terms("aggSubject").field("subject.keyword")
			.subAggregation(AggregationBuilders.cardinality("aggCountPerSubject").field("messageId"))
			.subAggregation(AggregationBuilders
				.terms("aggStatus").field("status")
				.subAggregation(AggregationBuilders.cardinality("aggCountPerStatus").field("messageId"))
			);

		SearchRequest searchRequest = new SearchRequest(eventIndex);
		searchRequest.source(searchSourceBuilder(mainQuery, aggregation));

		SearchResponse response = search(searchRequest);
		ParsedStringTerms aggSubject = response.getAggregations().get("aggSubject");
		for (Object itemSubject : aggSubject.getBuckets()) {
			ParsedStringTerms.ParsedBucket subjectBucket = (ParsedStringTerms.ParsedBucket) itemSubject;
			String subjectName = subjectBucket.getKeyAsString().toLowerCase();

			// Total per subject
			ParsedCardinality countPerSubject = subjectBucket.getAggregations().get("aggCountPerSubject");
			String metricName = String.format("%s.event_total%s.%s", prefix, toLabel(today), subjectName);
			map.get(metricName).set(countPerSubject.getValue());

			// Per each subject/status
			ParsedStringTerms aggStatus = subjectBucket.getAggregations().get("aggStatus");
			for (Object itemStatus : aggStatus.getBuckets()) {
				ParsedStringTerms.ParsedBucket statusBucket = (ParsedStringTerms.ParsedBucket) itemStatus;
				String statusName = statusBucket.getKeyAsString().toLowerCase();

				// Per subject/status
				ParsedCardinality aggCountPerStatus = statusBucket.getAggregations().get("aggCountPerStatus");
				metricName = String.format("%s.event_%s%s.%s", prefix, statusName, toLabel(today), subjectName);
				map.get(metricName).addAndGet(aggCountPerStatus.getValue());
			}
		}
	}

	private void taskTypeRefNameCounters(Map<String, AtomicLong> map, boolean today) {
		QueryBuilder typeQuery = QueryBuilders.termsQuery("taskType", TASK_TYPES);
		QueryBuilder statusQuery = QueryBuilders.termsQuery("status", TASK_STATUSES);
		BoolQueryBuilder mainQuery = QueryBuilders.boolQuery().must(typeQuery).must(statusQuery);
		if (today) {
			mainQuery = mainQuery.must(getStartTimeQuery());
		}

		TermsAggregationBuilder aggregation = AggregationBuilders
			.terms("aggTaskType").field("taskType")
			.subAggregation(AggregationBuilders
				.terms("aggRefName").field("referenceTaskName")
				.subAggregation(AggregationBuilders
					.terms("aggStatus").field("status")
				)
			);

		SearchRequest searchRequest = new SearchRequest(taskIndex);
		searchRequest.source(searchSourceBuilder(mainQuery, aggregation));

		SearchResponse response = search(searchRequest);
		ParsedStringTerms aggTaskType = response.getAggregations().get("aggTaskType");
		for (Object itemType : aggTaskType.getBuckets()) {
			ParsedStringTerms.ParsedBucket typeBucket = (ParsedStringTerms.ParsedBucket) itemType;
			String typeName = typeBucket.getKeyAsString().toLowerCase();

			ParsedStringTerms aggRefName = typeBucket.getAggregations().get("aggRefName");
			for (Object itemName : aggRefName.getBuckets()) {
				ParsedStringTerms.ParsedBucket refBucket = (ParsedStringTerms.ParsedBucket) itemName;
				String refName = refBucket.getKeyAsString().toLowerCase();

				// Init counters. Total per typeName/refName + today/non-today
				initMetric(map, String.format("%s.task_%s_%s", prefix, typeName, refName));
				initMetric(map, String.format("%s.task_%s_%s_today", prefix, typeName, refName));
				// Init counters. Per typeName/refName/status + today/non-today
				for (String status : TASK_STATUSES) {
					String statusName = status.toLowerCase();
					initMetric(map, String.format("%s.task_%s_%s_%s", prefix, typeName, refName, statusName));
					initMetric(map, String.format("%s.task_%s_%s_%s_today", prefix, typeName, refName, statusName));
				}

				// Per each refName/status
				ParsedStringTerms aggStatus = refBucket.getAggregations().get("aggStatus");
				for (Object itemStatus : aggStatus.getBuckets()) {
					ParsedStringTerms.ParsedBucket statusBucket = (ParsedStringTerms.ParsedBucket) itemStatus;
					String statusName = statusBucket.getKeyAsString().toLowerCase();
					long docCount = statusBucket.getDocCount();

					// Parent typeName + refName
					String metricName = String.format("%s.task_%s_%s%s", prefix, typeName, refName, toLabel(today));
					map.get(metricName).addAndGet(docCount);

					// typeName + refName + status
					metricName = String.format("%s.task_%s_%s_%s%s", prefix, typeName, refName, statusName, toLabel(today));
					map.get(metricName).addAndGet(docCount);
				}
			}
		}
	}

	private void taskTypeRefNameAverage(Map<String, AtomicLong> map, boolean today) {
		QueryBuilder typeQuery = QueryBuilders.termsQuery("taskType", TASK_TYPES);
		QueryBuilder statusQuery = QueryBuilders.termsQuery("status", Task.Status.COMPLETED);
		BoolQueryBuilder mainQuery = QueryBuilders.boolQuery().must(typeQuery).must(statusQuery);
		if (today) {
			mainQuery = mainQuery.must(getStartTimeQuery());
		}

		TermsAggregationBuilder aggregation = AggregationBuilders
			.terms("aggTaskType").field("taskType")
			.subAggregation(AggregationBuilders
				.terms("aggRefName").field("referenceTaskName")
				.subAggregation(averageExecTime));

		SearchRequest searchRequest = new SearchRequest(taskIndex);
		searchRequest.source(searchSourceBuilder(mainQuery, aggregation));

		SearchResponse response = search(searchRequest);
		ParsedStringTerms aggTaskType = response.getAggregations().get("aggTaskType");
		for (Object item : aggTaskType.getBuckets()) {
			ParsedStringTerms.ParsedBucket typeBucket = (ParsedStringTerms.ParsedBucket) item;
			String typeName = typeBucket.getKeyAsString().toLowerCase();

			ParsedStringTerms aggRefName = typeBucket.getAggregations().get("aggRefName");
			for (Object item2 : aggRefName.getBuckets()) {
				ParsedStringTerms.ParsedBucket refBucket = (ParsedStringTerms.ParsedBucket) item2;
				String refName = refBucket.getKeyAsString().toLowerCase();

				// Init both counters right away if any today/non-today returned
				initMetric(map, String.format("%s.avg_task_execution_msec.%s_%s", prefix, typeName, refName));
				initMetric(map, String.format("%s.avg_task_execution_msec_today.%s_%s", prefix, typeName, refName));

				// Per each refName/status
				ParsedAvg aggAvg = refBucket.getAggregations().get("aggAvg");
				double avg = Double.isInfinite(aggAvg.getValue()) ? 0 : aggAvg.getValue();

				String metricName = String.format("%s.avg_task_execution_msec%s.%s_%s", prefix, toLabel(today), typeName, refName);
				map.put(metricName, new AtomicLong(Math.round(avg)));
			}
		}
	}

	private void workflowCounters(Map<String, AtomicLong> map, boolean today, String shortName, Set<String> filtered) {
		List<String> workflowStatuses = today ? WORKFLOW_TODAY_STATUSES : WORKFLOW_OVERALL_STATUSES;

		// Init counters
		initMetric(map, String.format("%s.workflow_started%s", prefix, toLabel(today)));

		// Counter name per status
		for (String status : workflowStatuses) {
			initMetric(map, String.format("%s.workflow_%s%s", prefix, status.toLowerCase(), toLabel(today)));
		}

		// Counter name per workflow type and status
		initMetric(map, String.format("%s.workflow_started%s.%s", prefix, toLabel(today), shortName));
		for (String status : workflowStatuses) {
			String metricName = String.format("%s.workflow_%s%s.%s", prefix, status.toLowerCase(), toLabel(today), shortName);
			initMetric(map, metricName);
		}

		// Build the today/not-today query
		QueryBuilder typeQuery = QueryBuilders.termsQuery("workflowType", filtered);
		QueryBuilder statusQuery = QueryBuilders.termsQuery("status", workflowStatuses);
		BoolQueryBuilder mainQuery = QueryBuilders.boolQuery().must(typeQuery).must(statusQuery);
		if (today) {
			mainQuery = mainQuery.must(getStartTimeQuery());
		}

		// Aggregation by workflow type and sub aggregation by workflow status
		TermsAggregationBuilder aggregation = AggregationBuilders
			.terms("aggWorkflowType").field("workflowType")
			.subAggregation(AggregationBuilders.terms("aggStatus").field("status"));

		SearchRequest searchRequest = new SearchRequest(workflowIndex);
		searchRequest.source(searchSourceBuilder(mainQuery, aggregation));

		SearchResponse response = search(searchRequest);

		ParsedStringTerms aggWorkflowType = response.getAggregations().get("aggWorkflowType");
		for (Object item : aggWorkflowType.getBuckets()) {
			ParsedStringTerms.ParsedBucket typeBucket = (ParsedStringTerms.ParsedBucket) item;

			// Total started
			String metricName = String.format("%s.workflow_started%s", prefix, toLabel(today));
			map.get(metricName).addAndGet(typeBucket.getDocCount());

			// Started per workflow type
			metricName = String.format("%s.workflow_started%s.%s", prefix, toLabel(today), shortName);
			map.get(metricName).addAndGet(typeBucket.getDocCount());

			// Per each workflow/status
			ParsedStringTerms aggStatus = typeBucket.getAggregations().get("aggStatus");
			for (Object statusItem : aggStatus.getBuckets()) {
				ParsedStringTerms.ParsedBucket statusBucket = (ParsedStringTerms.ParsedBucket) statusItem;
				String statusName = statusBucket.getKeyAsString().toLowerCase();
				long docCount = statusBucket.getDocCount();

				// Counter name per status
				metricName = String.format("%s.workflow_%s%s", prefix, statusName, toLabel(today));
				map.get(metricName).addAndGet(docCount);

				// Counter name per workflow type and status
				metricName = String.format("%s.workflow_%s%s.%s", prefix, statusName, toLabel(today), shortName);
				map.get(metricName).addAndGet(docCount);
			}
		}
	}

	private void workflowAverage(Map<String, AtomicLong> map, boolean today, String shortName, Set<String> filtered) {
		QueryBuilder typesQuery = QueryBuilders.termsQuery("workflowType", filtered);
		QueryBuilder statusQuery = QueryBuilders.termQuery("status", Workflow.WorkflowStatus.COMPLETED);
		BoolQueryBuilder mainQuery = QueryBuilders.boolQuery().must(typesQuery).must(statusQuery);
		if (today) {
			mainQuery = mainQuery.must(getStartTimeQuery());
		}

		SearchRequest searchRequest = new SearchRequest(workflowIndex);
		searchRequest.source(searchSourceBuilder(mainQuery, averageExecTime));

		SearchResponse response = search(searchRequest);
		ParsedAvg aggAvg = response.getAggregations().get("aggAvg");

		double avg = Double.isInfinite(aggAvg.getValue()) ? 0 : aggAvg.getValue();

		String metricName = String.format("%s.avg_workflow_execution_sec%s.%s", prefix, toLabel(today), shortName);
		map.put(metricName, new AtomicLong(Math.round(avg / 1000)));
	}

	private void overallWorkflowAverage(Map<String, AtomicLong> map, boolean today, Set<String> fullNames) {
		QueryBuilder typesQuery = QueryBuilders.termsQuery("workflowType", fullNames);
		QueryBuilder statusQuery = QueryBuilders.termQuery("status", "COMPLETED");
		BoolQueryBuilder mainQuery = QueryBuilders.boolQuery().must(typesQuery).must(statusQuery);
		if (today) {
			mainQuery = mainQuery.must(getStartTimeQuery());
		}

		SearchRequest searchRequest = new SearchRequest(workflowIndex);
		searchRequest.source(searchSourceBuilder(mainQuery, averageExecTime));

		SearchResponse response = search(searchRequest);
		ParsedAvg aggAvg = response.getAggregations().get("aggAvg");

		double avg = Double.isInfinite(aggAvg.getValue()) ? 0 : aggAvg.getValue();

		String metricName = String.format("%s.avg_workflow_execution_sec%s", prefix, toLabel(today));
		map.put(metricName, new AtomicLong(Math.round(avg / 1000)));
	}

	private SearchResponse search(SearchRequest request) {
		try {
			return client.search(request);
		} catch (IOException e) {
			logger.warn("search failed for " + request + " " + e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private long getPstStartTime() {
		TimeZone pst = TimeZone.getTimeZone("America/Los_Angeles");

		Calendar calendar = new GregorianCalendar();
		calendar.setTimeZone(pst);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		return calendar.getTimeInMillis();
	}

	private SearchSourceBuilder searchSourceBuilder(QueryBuilder query, AggregationBuilder aggregation) {
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.aggregation(aggregation);
		sourceBuilder.fetchSource(false);
		sourceBuilder.query(query);
		sourceBuilder.size(0);

		return sourceBuilder;
	}

	private QueryBuilder getStartTimeQuery() {
		return getStartTimeQuery("startTime");
	}

	private QueryBuilder getStartTimeQuery(String field) {
		return QueryBuilders.rangeQuery(field).gte(getPstStartTime());
	}

	private void initMetric(Map<String, AtomicLong> map, String metricName) {
		map.computeIfAbsent(metricName, s -> new AtomicLong(0));
	}

	private String toLabel(boolean today) {
		return today ? "_today" : "";
	}
}