package com.netflix.conductor.aurora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.aurora.sql.ResultSetHandler;
import com.netflix.conductor.common.run.ErrorLookup;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.dao.ErrorLookupDAO;
import com.netflix.conductor.dao.ExecutionDAO;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuroraErrorLookupDAO extends AuroraBaseDAO implements ErrorLookupDAO {

	private ExecutionDAO executionDAO;

	@Inject
	public AuroraErrorLookupDAO(DataSource dataSource, ObjectMapper mapper, ExecutionDAO executionDAO, Configuration config) {
		super(dataSource, mapper);
		this.executionDAO = executionDAO;
	}

	@Override
	public List<ErrorLookup> getErrors() {

		return getWithTransaction( tx->{
            ErrorLookupHandler handler = new ErrorLookupHandler();

			String SQL = "SELECT * FROM meta_error_registry ORDER BY id";
			return query( tx, SQL, q->q.executeAndFetch(handler));
		});
	}

	@Override
	public boolean addError(int id, String errorCode, String lookup, String workflowName, String generalMessage, String root_cause, String resolution) {
		return getWithTransaction( tx->{

			String SQL = "INSERT INTO meta_error_registry (id, error_code, lookup, workflow_name, general_message, root_cause, resolution) values (?,?,?,?,?,?,?)";

			return query( tx, SQL, q->q.addParameter(id)
			        .addParameter(errorCode)
			        .addParameter(lookup)
                    .addParameter(workflowName)
                    .addParameter(generalMessage)
                    .addParameter(root_cause)
        			.addParameter(resolution)
		        	.executeUpdate() > 0);
		});
	}

	@Override
	public List<ErrorLookup> getErrorMatching(String errorString) {
		return getWithTransaction( tx->{
            ErrorLookupHandler handler = new ErrorLookupHandler();

			StringBuilder SQL = new StringBuilder("select * from ( ");
			SQL.append("select substring(?, lookup) as matched_txt, * ");
			SQL.append("from meta_error_registry ");
			SQL.append(") as match_results ");
			SQL.append("where matched_txt is not null ");
			SQL.append("order by length(matched_txt) desc ");

			return query( tx, SQL.toString(), q-> q.addParameter(errorString).executeAndFetch(handler));

		});
	}

	class ErrorLookupHandler implements ResultSetHandler<ArrayList<ErrorLookup>> {

        public ArrayList<ErrorLookup> apply(ResultSet rs) throws SQLException {
            ArrayList<ErrorLookup> errorLookups = new ArrayList<>();

            while( rs.next()){
                ErrorLookup errorLookup = new ErrorLookup();
                errorLookup.setId( rs.getInt("id"));
                errorLookup.setErrorCode( rs.getString("error_code"));
                errorLookup.setLookup(rs.getString("lookup"));
                errorLookup.setWorkflowName(rs.getString("workflow_name"));
                errorLookup.setGeneralMessage(rs.getString("general_message"));
                errorLookup.setRootCause( rs.getString("root_cause"));
                errorLookup.setResolution( rs.getString("resolution"));
                errorLookups.add(errorLookup);
            }
            return errorLookups;
        }
    }

//
//	private void eventExecAverage(Map<String, AtomicLong> map, boolean today) {
//		Set<String> subjects = getSubjects();
//		List<String> statuses = Arrays.asList("COMPLETED", "FAILED");
//
//		for (String subject : subjects) {
//			initMetric(map, String.format("%s.avg_event_exec_msec%s.%s", PREFIX, toLabel(today), subject.toLowerCase()));
//		}
//
//		withTransaction(tx -> {
//			ResultSetHandler<Object> handler = rs -> {
//				while (rs.next()) {
//					String subject = rs.getString("subject").toLowerCase();
//					long avg = rs.getLong("avg");
//
//					String metricName = String.format("%s.avg_event_exec_msec%s.%s", PREFIX, toLabel(today), subject.toLowerCase());
//					map.get(metricName).set(avg);
//				}
//				return null;
//			};
//
//			StringBuilder SQL = new StringBuilder("SELECT subject, avg(extract('epoch' from processed_on) - extract('epoch' from started_on)) as avg ");
//			SQL.append("FROM event_execution  ");
//			SQL.append("WHERE processed_on IS NOT NULL AND started_on IS NOT NULL ");
//			SQL.append("AND subject = ANY(?) AND status = ANY(?) ");
//			if (today) {
//				SQL.append("AND received_on >= ? ");
//				SQL.append("GROUP BY subject");
//
//				query(tx, SQL.toString(), q -> q.addParameter(subjects).addParameter(statuses)
//					.addTimestampParameter(getStartTime())
//					.executeAndFetch(handler));
//			} else {
//				SQL.append("GROUP BY subject");
//
//				query(tx, SQL.toString(), q -> q.addParameter(subjects).addParameter(statuses)
//					.executeAndFetch(handler));
//			}
//		});
//	}
//
//	private void eventPublished(Map<String, AtomicLong> map, boolean today) {
//		for (String subject : SINK_SUBJECTS) {
//			initMetric(map, String.format("%s.event_published%s.%s", PREFIX, toLabel(today), subject.toLowerCase()));
//		}
//
//		withTransaction(tx -> {
//			ResultSetHandler<Object> handler = rs -> {
//				while (rs.next()) {
//					String subject = rs.getString("subject").toLowerCase();
//					long count = rs.getLong("count");
//
//					String metricName = String.format("%s.event_published%s.%s", PREFIX, toLabel(today), subject.toLowerCase());
//					map.get(metricName).set(count);
//				}
//				return null;
//			};
//
//			StringBuilder SQL = new StringBuilder("SELECT subject, count(*) as count FROM event_published ");
//			SQL.append("WHERE subject = ANY(?) ");
//			if (today) {
//				SQL.append("AND published_on >= ? ");
//				SQL.append("GROUP BY subject");
//
//				query(tx, SQL.toString(), q -> q.addParameter(SINK_SUBJECTS)
//					.addTimestampParameter(getStartTime())
//					.executeAndFetch(handler));
//			} else {
//				SQL.append("GROUP BY subject");
//
//				query(tx, SQL.toString(), q -> q.addParameter(SINK_SUBJECTS).executeAndFetch(handler));
//			}
//		});
//	}
//
//	private void eventWaitAverage(Map<String, AtomicLong> map, boolean today) {
//		Set<String> subjects = getSubjects();
//
//		for (String subject : subjects) {
//			initMetric(map, String.format("%s.avg_event_wait_msec%s.%s", PREFIX, toLabel(today), subject.toLowerCase()));
//		}
//
//		withTransaction(tx -> {
//			ResultSetHandler<Object> handler = rs -> {
//				while (rs.next()) {
//					String subject = rs.getString("subject").toLowerCase();
//					long avg = rs.getLong("avg");
//
//					String metricName = String.format("%s.avg_event_wait_msec%s.%s", PREFIX, toLabel(today), subject.toLowerCase());
//					map.get(metricName).set(avg);
//				}
//				return null;
//			};
//
//			StringBuilder SQL = new StringBuilder("SELECT subject, avg(extract('epoch' from accepted_on) - extract('epoch' from received_on)) as avg ");
//			SQL.append("FROM event_execution  ");
//			SQL.append("WHERE accepted_on IS NOT NULL AND received_on IS NOT NULL ");
//			SQL.append("AND subject = ANY(?) AND status = ANY(?) ");
//			if (today) {
//				SQL.append("AND received_on >= ? ");
//				SQL.append("GROUP BY subject");
//
//				query(tx, SQL.toString(), q -> q.addParameter(subjects).addParameter(EVENT_STATUSES)
//					.addTimestampParameter(getStartTime())
//					.executeAndFetch(handler));
//			} else {
//				SQL.append("GROUP BY subject");
//
//				query(tx, SQL.toString(), q -> q.addParameter(subjects).addParameter(EVENT_STATUSES)
//					.executeAndFetch(handler));
//			}
//		});
//	}
//
//	private void taskTypeRefNameAverage(Map<String, AtomicLong> map, boolean today) {
//		withTransaction(tx -> {
//			ResultSetHandler<Object> handler = rs -> {
//				while (rs.next()) {
//					String typeName = rs.getString("task_type").toLowerCase();
//					String refName = rs.getString("task_refname").toLowerCase();
//					long avg = rs.getLong("avg");
//
//					// Init both counters right away if any today/non-today returned
//					initMetric(map, String.format("%s.avg_task_execution_msec.%s_%s", PREFIX, typeName, refName));
//					initMetric(map, String.format("%s.avg_task_execution_msec_today.%s_%s", PREFIX, typeName, refName));
//
//					// Se the correct one
//					String metricName = String.format("%s.avg_task_execution_msec%s.%s_%s", PREFIX, toLabel(today), typeName, refName);
//					map.get(metricName).set(avg);
//				}
//				return null;
//			};
//
//			StringBuilder SQL = new StringBuilder("SELECT task_type, task_refname, avg(extract('epoch' from end_time) - extract('epoch' from start_time)) as avg ");
//			SQL.append("FROM task WHERE start_time IS NOT NULL AND end_time IS NOT NULL ");
//			SQL.append("AND task_type = ANY(?) AND task_status = 'COMPLETED' ");
//			if (today) {
//				SQL.append("AND start_time >= ? ");
//				SQL.append("GROUP BY task_type, task_refname");
//
//				query(tx, SQL.toString(), q -> q.addParameter(TASK_TYPES)
//					.addTimestampParameter(getStartTime())
//					.executeAndFetch(handler));
//			} else {
//				SQL.append("GROUP BY task_type, task_refname");
//
//				query(tx, SQL.toString(), q -> q.addParameter(TASK_TYPES)
//					.executeAndFetch(handler));
//			}
//		});
//	}
//
//	private void taskTypeRefNameCounters(Map<String, AtomicLong> map, boolean today) {
//		withTransaction(tx -> {
//			ResultSetHandler<Object> handler = rs -> {
//				while (rs.next()) {
//					String typeName = rs.getString("task_type").toLowerCase();
//					String refName = rs.getString("task_refname").toLowerCase();
//					String status = rs.getString("task_status").toLowerCase();
//					long count = rs.getLong("count");
//
//					// Init counters. Total per typeName/refName + today/non-today
//					initMetric(map, String.format("%s.task_%s_%s", PREFIX, typeName, refName));
//					initMetric(map, String.format("%s.task_%s_%s_today", PREFIX, typeName, refName));
//
//					// Init counters. Per typeName/refName/status + today/non-today
//					for (String statusName : TASK_STATUSES) {
//						initMetric(map, String.format("%s.task_%s_%s_%s", PREFIX, typeName, refName, statusName.toLowerCase()));
//						initMetric(map, String.format("%s.task_%s_%s_%s_today", PREFIX, typeName, refName, statusName.toLowerCase()));
//					}
//
//					// Parent typeName + refName
//					String metricName = String.format("%s.task_%s_%s%s", PREFIX, typeName, refName, toLabel(today));
//					map.get(metricName).addAndGet(count);
//
//					// typeName + refName + status
//					metricName = String.format("%s.task_%s_%s_%s%s", PREFIX, typeName, refName, status, toLabel(today));
//					map.get(metricName).addAndGet(count);
//				}
//				return null;
//			};
//
//			StringBuilder SQL = new StringBuilder("SELECT task_type, task_refname, task_status, count(*) as count ");
//			SQL.append("FROM task WHERE task_type = ANY(?) AND task_status = ANY(?) ");
//			if (today) {
//				SQL.append("AND start_time >= ? ");
//				SQL.append("GROUP BY task_type, task_refname, task_status");
//
//				query(tx, SQL.toString(), q -> q.addParameter(TASK_TYPES).addParameter(TASK_STATUSES)
//					.addTimestampParameter(getStartTime())
//					.executeAndFetch(handler));
//			} else {
//				SQL.append("GROUP BY task_type, task_refname, task_status");
//
//				query(tx, SQL.toString(), q -> q.addParameter(TASK_TYPES).addParameter(TASK_STATUSES)
//					.executeAndFetch(handler));
//			}
//		});
//	}
//
//    private void taskTypeCounters(Map<String, AtomicLong> map, boolean today) {
//        withTransaction(tx -> {
//            ResultSetHandler<Object> handler = rs -> {
//                while (rs.next()) {
//                    String typeName = rs.getString("task_type").toLowerCase();
//                    String status = rs.getString("task_status").toLowerCase();
//                    long count = rs.getLong("count");
//
//                    // Init counters. Total per typeName + today/non-today
//                    initMetric(map, String.format("%s.task_%s", PREFIX, typeName));
//                    initMetric(map, String.format("%s.task_%s_today", PREFIX, typeName));
//
//                    // Init counters. Per typeName/status + today/non-today
//                    for (String statusName : TASK_STATUSES) {
//                        initMetric(map, String.format("%s.task_%s_%s", PREFIX, typeName, statusName.toLowerCase()));
//                        initMetric(map, String.format("%s.task_%s_%s_today", PREFIX, typeName, statusName.toLowerCase()));
//                    }
//
//                    // Parent typeName
//                    String metricName = String.format("%s.task_%s%s", PREFIX, typeName, toLabel(today));
//                    map.get(metricName).addAndGet(count);
//
//                    // typeName + status
//                    metricName = String.format("%s.task_%s_%s%s", PREFIX, typeName, status, toLabel(today));
//                    map.get(metricName).addAndGet(count);
//                }
//                return null;
//            };
//
//            StringBuilder SQL = new StringBuilder("SELECT task_type, task_status, count(*) as count ");
//            SQL.append("FROM task WHERE task_type = ANY(?) AND task_status = ANY(?) ");
//            if (today) {
//                SQL.append("AND start_time >= ? ");
//                SQL.append("GROUP BY task_type, task_status");
//
//                query(tx, SQL.toString(), q -> q.addParameter(TASK_TYPES).addParameter(TASK_STATUSES)
//                        .addTimestampParameter(getStartTime())
//                        .executeAndFetch(handler));
//            } else {
//                SQL.append("GROUP BY task_type, task_status");
//
//                query(tx, SQL.toString(), q -> q.addParameter(TASK_TYPES).addParameter(TASK_STATUSES)
//                        .executeAndFetch(handler));
//            }
//        });
//    }
//
//	private void workflowAverage(Map<String, AtomicLong> map, boolean today, String shortName, Set<String> filtered) {
//		withTransaction(tx -> {
//			ResultSetHandler<Object> handler = rs -> {
//				while (rs.next()) {
//					long avg = rs.getLong("avg");
//
//					String metricName = String.format("%s.avg_workflow_execution_msec%s.%s", PREFIX, toLabel(today), shortName);                    // Se the correct one
//					map.put(metricName, new AtomicLong(avg));
//				}
//				return null;
//			};
//
//			StringBuilder SQL = new StringBuilder("SELECT avg(extract('epoch' from end_time) - extract('epoch' from start_time)) as avg ");
//			SQL.append("FROM workflow WHERE start_time IS NOT NULL AND end_time IS NOT NULL ");
//			SQL.append("AND workflow_type = ANY(?) AND workflow_status = 'COMPLETED' ");
//			if (today) {
//				SQL.append("AND start_time >= ? ");
//
//				query(tx, SQL.toString(), q -> q.addParameter(filtered)
//					.addTimestampParameter(getStartTime())
//					.executeAndFetch(handler));
//			} else {
//				query(tx, SQL.toString(), q -> q.addParameter(filtered)
//					.executeAndFetch(handler));
//			}
//		});
//	}
//
//
//	private void execWorkflowActionAverage(Map<String, AtomicLong> map) {
//		withTransaction(tx -> {
//			ResultSetHandler<Object> handler = rs -> {
//				while (rs.next()) {
//					long avg = rs.getLong("avg_time_taken");
//					String metricName = String.format("%s.avg_execproc_workflow_msec%s.%s", PREFIX, toLabel(true), rs.getString("action_type"));
//					map.put(metricName, new AtomicLong(avg));
//				}
//				return null;
//			};
//
//            StringBuilder SQL = new StringBuilder("SELECT input::json->>'action' as action_type, avg(extract('epoch' from end_time) - extract('epoch' from start_time)) as avg_time_taken ");
//            SQL.append("FROM workflow WHERE start_time IS NOT NULL AND end_time IS NOT NULL ");
//			SQL.append("AND workflow_type like 'deluxe.dependencygraph.execute.process%' AND workflow_status = 'COMPLETED' ");
//			SQL.append("AND start_time >= ? ");
//			SQL.append("group by input::json->>'action' ");
//
//			query(tx, SQL.toString(), q -> q.addTimestampParameter(getStartTime()).executeAndFetch(handler));
//		});
//	}
//
//	private void workflowCounters(Map<String, AtomicLong> map, boolean today, String shortName, Set<String> filtered) {
//		List<String> workflowStatuses = today ? WORKFLOW_TODAY_STATUSES : WORKFLOW_OVERALL_STATUSES;
//
//		// Init counters
//		initMetric(map, String.format("%s.workflow_started%s", PREFIX, toLabel(today)));
//
//		// Counter name per status
//		for (String status : workflowStatuses) {
//			initMetric(map, String.format("%s.workflow_%s%s", PREFIX, status.toLowerCase(), toLabel(today)));
//		}
//
//		// Counter name per workflow type and status
//		initMetric(map, String.format("%s.workflow_started%s.%s", PREFIX, toLabel(today), shortName));
//		for (String status : workflowStatuses) {
//			String metricName = String.format("%s.workflow_%s%s.%s", PREFIX, status.toLowerCase(), toLabel(today), shortName);
//			initMetric(map, metricName);
//		}
//
//		withTransaction(tx -> {
//			ResultSetHandler<Object> handler = rs -> {
//				while (rs.next()) {
//					String typeName = rs.getString("workflow_type").toLowerCase();
//					String statusName = rs.getString("workflow_status").toLowerCase();
//					long count = rs.getLong("count");
//
//					// Total started
//					String metricName = String.format("%s.workflow_started%s", PREFIX, toLabel(today));
//					map.get(metricName).addAndGet(count);
//
//					// Started per workflow type
//					metricName = String.format("%s.workflow_started%s.%s", PREFIX, toLabel(today), shortName);
//					map.get(metricName).addAndGet(count);
//
//					// Counter name per status
//					metricName = String.format("%s.workflow_%s%s", PREFIX, statusName, toLabel(today));
//					map.get(metricName).addAndGet(count);
//
//					// Counter name per workflow type and status
//					metricName = String.format("%s.workflow_%s%s.%s", PREFIX, statusName, toLabel(today), shortName);
//					map.get(metricName).addAndGet(count);
//
//				}
//				return null;
//			};
//
//			StringBuilder SQL = new StringBuilder("SELECT workflow_type, workflow_status, count(*) as count ");
//			SQL.append("FROM workflow WHERE start_time IS NOT NULL AND end_time IS NOT NULL ");
//			SQL.append("AND workflow_type = ANY(?) AND workflow_status = ANY(?) ");
//			if (today) {
//				SQL.append("AND start_time >= ? ");
//				SQL.append("GROUP BY workflow_type, workflow_status");
//
//				query(tx, SQL.toString(), q -> q.addParameter(filtered).addParameter(workflowStatuses)
//					.addTimestampParameter(getStartTime())
//					.executeAndFetch(handler));
//			} else {
//				SQL.append("GROUP BY workflow_type, workflow_status");
//
//				query(tx, SQL.toString(), q -> q.addParameter(filtered).addParameter(workflowStatuses)
//					.executeAndFetch(handler));
//			}
//		});
//	}
}
