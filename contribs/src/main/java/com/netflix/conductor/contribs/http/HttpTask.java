/**
 * Copyright 2016 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 *
 */
package com.netflix.conductor.contribs.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.auth.AuthManager;
import com.netflix.conductor.auth.ForeignAuthManager;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.Task.Status;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.core.DNSLookup;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.events.ScriptEvaluator;
import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.dao.ExecutionDAO;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.service.MetricService;
import datadog.trace.api.Trace;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.isNull;

/**
 * @author Viren Task that enables calling another http endpoint as part of its
 *         execution
 */
@Singleton
public class HttpTask extends GenericHttpTask {
	private static final Logger logger = LoggerFactory.getLogger(HttpTask.class);
	static final String MISSING_REQUEST = "Missing HTTP request. Task input MUST have a '" + REQUEST_PARAMETER_NAME
			+ "' key wiht HttpTask.Input as value. See documentation for HttpTask for required input parameters";
	public static final String NAME = "HTTP";
	private static final String CONDITIONS_PARAMETER = "conditions";
	static final String LONG_RUNNING_HTTP = "long_running_http";
	private final int unackTimeout;
    private final long initialDelay;
	private final long updateDelay;
	private final QueueDAO queue;
	private final ExecutionDAO executionDAO;
	private ScheduledExecutorService executorService ;

	@Inject
	public HttpTask(RestClientManager rcm, Configuration config, ObjectMapper om, AuthManager authManager, ForeignAuthManager foreignAuthManager, QueueDAO queue, ExecutionDAO executionDAO) {
		super(NAME, config, rcm, om, authManager, foreignAuthManager);
		this.queue =queue;
		this.executionDAO = executionDAO;
		unackTimeout = config.getIntProperty("workflow.system.task.http.unack.timeout", 60);
		long initialDelayCfg = config.getIntProperty("workflow.system.task.http.long.unack.initialDelay", 0);
		long updateDelayCfg = config.getIntProperty("workflow.system.task.http.long.unack.updateDelay", 0);
		long unackTimeoutMs = unackTimeout * 1000;
		initialDelay = (initialDelayCfg <= 0 || initialDelayCfg >= unackTimeoutMs) ? unackTimeoutMs / 2 : initialDelayCfg;
		updateDelay  = (updateDelayCfg <= 0  || updateDelayCfg  >= unackTimeoutMs) ? unackTimeoutMs : updateDelayCfg;

		executorService = Executors.newScheduledThreadPool(25);
		logger.debug("HttpTask initialized...");
	}

	@Trace(operationName = "Start Http Task", resourceName = "httpTask")
	@Override
	public void start(Workflow workflow, Task task, WorkflowExecutor executor) throws Exception {

		Instant start = Instant.now();
		Object request = task.getInputData().get(REQUEST_PARAMETER_NAME);
		task.setWorkerId(config.getServerId());
		String hostAndPort = null;
		Input input = om.convertValue(request, Input.class);

		if (request == null) {
			task.setReasonForIncompletion(MISSING_REQUEST);
			task.setStatus(Status.FAILED);
			return;
		} else if (StringUtils.isNotEmpty(input.getServiceDiscoveryQuery())) {
			hostAndPort = DNSLookup.lookup(input.getServiceDiscoveryQuery());

			if (null == hostAndPort) {
				final String msg = "Service discovery failed for: " + input.getServiceDiscoveryQuery()
						+ " . No records found.";
				logger.error(msg);
				executionDAO.addAlert(msg);
				task.setStatus(Status.FAILED);
				task.setReasonForIncompletion(msg);
				task.getOutputData().put("response",msg);
				return;
			}
		}

		if (StringUtils.isEmpty(input.getUri())) {
			task.setReasonForIncompletion(
					"Missing HTTP URI. See documentation for HttpTask for required input parameters");
			task.setStatus(Status.FAILED);
			return;
		} else if (StringUtils.isNotEmpty(hostAndPort)) {
			final String uri = input.getUri();

			if (uri.startsWith("/")) {
				input.setUri(hostAndPort + uri);
			} else {
				// https://jira.d3nw.com/browse/ONECOND-837
				// Parse URI, extract the path, and append it to url
				try {
					URL tmp = new URL(uri);
					input.setUri(hostAndPort + tmp.getPath());
				} catch (MalformedURLException e) {
					logger.error("Unable to build endpoint URL: " + uri, e);
					throw new Exception("Unable to build endpoint URL: " + uri, e);
				}
			}
		} else if (StringUtils.isNotEmpty(input.getUri())) {
			// Do Nothing, use input.getUri() as is
		}

		if (input.getMethod() == null) {
			task.setReasonForIncompletion("No HTTP method specified");
			task.setStatus(Status.FAILED);
			return;
		}

		String serviceName = input.getServiceDiscoveryQuery();
		if (StringUtils.isEmpty(serviceName)) {
			serviceName = new URL(input.getUri()).getHost();
		}

		long start_time = System.currentTimeMillis();
		ScheduledFuture<?> scheduledFuture = null;
		try {
			HttpResponse response = new HttpResponse();
			if("checkvfsid".equalsIgnoreCase(task.getReferenceTaskName())) {
			logger.debug("http task starting. WorkflowId=" + workflow.getWorkflowId()
					+ ",taskReferenceName=" + task.getReferenceTaskName()
					+ ",service=" + serviceName
					+ ",taskId=" + task.getTaskId()
					+ ",url=" + input.getUri()
					+ ",correlationId=" + workflow.getCorrelationId()
					+ ",traceId=" + workflow.getTraceId()
					+ ",contextUser=" + workflow.getContextUser());
			}
			Object isLongRunningTask = task.getInputData().get(LONG_RUNNING_HTTP);
			if (isLongRunningTask != null && Boolean.valueOf(isLongRunningTask.toString())) {
				scheduledFuture = executorService.scheduleWithFixedDelay(() -> updateUnack(task.getTaskId()), initialDelay, updateDelay, TimeUnit.MILLISECONDS);
			}

			if (input.getContentType() != null) {
				if (input.getContentType().equalsIgnoreCase("application/x-www-form-urlencoded")) {
					String json = om.writeValueAsString(task.getInputData());
					JSONObject obj = new JSONObject(json);
					JSONObject getSth = obj.getJSONObject("http_request");

					Object main_body = getSth.get("body");
					String body = main_body.toString();

					response = httpCallUrlEncoded(input, body);

				} else {
					response = httpCall(input, task, workflow, executor);
				}
			} else {
				response = httpCall(input, task, workflow, executor);
			}

			// true - means status been handled, otherwise should apply the original logic
			boolean handled = handleStatusMapping(task, response);
			if (!handled) {
				handled = handleResponseMapping(task, response);
				if (!handled) {
					if (response.statusCode > 199 && response.statusCode < 300) {
						task.setStatus(Status.COMPLETED);
					} else {
						task.setStatus(Task.Status.FAILED);
					}
				}
			}

			// Check the http response validation. It will overwrite the task status if needed
			if (task.getStatus() == Status.COMPLETED) {
				checkHttpResponseValidation(task, response);
			} else {
				ResponseFailureReason failureReasonParam = getResponseFailureField(task);
				if (isNull(failureReasonParam)) {
					setReasonForIncompletion(response, task);
				} else {
					setCustomFailureReasonForIncompletion(task, failureReasonParam, response);
				}
			}
			handleResetStartTime(task, executor);

			task.getOutputData().put("response", response.asMap());

			long exec_time = System.currentTimeMillis() - start_time;
			/*logger.info("http task completed. WorkflowId=" + workflow.getWorkflowId()
					+ ",taskReferenceName=" + task.getReferenceTaskName()
					+ ",service=" + serviceName
					+ ",taskId=" + task.getTaskId()
					+ ",url=" + input.getUri()
					+ ",executeTimeMs=" + exec_time
					+ ",statusCode=" + response.statusCode
					+ ",correlationId=" + workflow.getCorrelationId()
					+ ",contextUser=" + workflow.getContextUser()
					+ ",traceId=" + workflow.getTraceId()
					+ ",request=" + input.getBody());*/
			MetricService.getInstance().httpExecution(task.getTaskType(),
					task.getReferenceTaskName(),
					task.getTaskDefName(),
					serviceName,
					Duration.between(start, Instant.now()).toMillis());
		} catch (Exception ex) {
			long exec_time = System.currentTimeMillis() - start_time;
			logger.error("http task failed. WorkflowId=" + workflow.getWorkflowId()
					+ ",taskReferenceName=" + task.getReferenceTaskName()
					+ ",service=" + serviceName
					+ ",taskId=" + task.getTaskId()
					+ ",url=" + input.getUri()
					+ ",executeTimeMs=" + exec_time
					+ ",correlationId=" + workflow.getCorrelationId()
					+ ",contextUser=" + workflow.getContextUser() + " with " + ex.getMessage(), ex);
			task.setStatus(Status.FAILED);
			task.setReasonForIncompletion(ex.getMessage());
			task.getOutputData().put("response", ex.getMessage());
			MetricService.getInstance().httpFailed(task.getTaskType(),
					task.getReferenceTaskName(),
					task.getTaskDefName(),
					serviceName,
					exec_time);
		}finally{
			if (scheduledFuture != null)
				scheduledFuture.cancel(true);
		}
	}

	public void shutdown() {
		try {
			if (executorService != null) {
				logger.info("Closing updateUnack pool");
				executorService.shutdown();
				executorService.awaitTermination(5, TimeUnit.SECONDS);
			}
		} catch (Exception e) {
			logger.error("Closing updateUnack pool failed " + e.getMessage(), e);
		}
	}


	public void updateUnack(String taskId)
	{
		queue.setUnackTimeout("http", taskId, unackTimeout * 1000L);
	}

	@Override
	public boolean execute(Workflow workflow, Task task, WorkflowExecutor executor) throws Exception {
		return false;
	}

	@Override
	public void cancel(Workflow workflow, Task task, WorkflowExecutor executor) throws Exception {
		task.setStatus(Status.CANCELED);
	}

	@Override
	public boolean isAsync() {
		return true;
	}

	@Override
	public int getRetryTimeInSecond() {
		return unackTimeout;
	}

	@SuppressWarnings("unchecked")
	private void addEvalResult(Task task, String condition, Object result) {
		Map<String, Object> taskOutput = task.getOutputData();
		Map<String, Object> conditions = (Map<String, Object>) taskOutput.get(CONDITIONS_PARAMETER);
		if (conditions == null) {
			conditions = new HashMap<>();
			taskOutput.put(CONDITIONS_PARAMETER, conditions);
		}
		conditions.put(condition, result);
	}

	/**
	 * Retrieves response reason failure configuration
	 * @param task the task being executed
	 * @return the failure reason configuration field
	 */
	private ResponseFailureReason getResponseFailureField(Task task) {
		Object responseParam = task.getInputData().get(RESPONSE_PARAMETER_NAME);
		// Check http_response object is present or not
		if (isNull(responseParam)) return null;

		Output output = om.convertValue(responseParam, Output.class);
		return isNull(output) ? null : output.getResponseFailureReason();
	}

	/**
	 * Allows systems to provision a custom failure reason based of the HTTP response body
	 * <p>
	 * This is specifically to provide flexibility and prevent large failure payload
	 * @param task the task being executed
	 * @param failureReasonParam the configured field to retrieve failure reason from
	 * @param response the HTTP response from the remote server
	 */
	private void setCustomFailureReasonForIncompletion(Task task, ResponseFailureReason failureReasonParam, HttpResponse response) {
		String failureReasonField = failureReasonParam.getField();
		if (StringUtils.isEmpty(failureReasonField)) { // for cases of missing reason field, we'll default back to the full response body
			logger.error(String.format("the response custom failure reason field is missing for this workflow, see details: task id = %s, workflow id = %s",
					task.getTaskId(), task.getWorkflowInstanceId())); // log this so we can track bad configuration (possibly emit a statsD metric for datadog alerts)
			setReasonForIncompletion(response, task);
			return;
		}

		// Get the response map with details of the failure reason or default to full response body if non exists
		Map<String, Object> responseMap = response.asMap();
		if (isNull(responseMap) || responseMap.isEmpty()) {
			logger.warn(String.format("no response body from remote server to evaluate failure reason, see details: task id = %s, workflow id = %s",
					task.getTaskId(), task.getWorkflowInstanceId()));
			setReasonForIncompletion(response, task);
			return;
		}

		// evaluate the provided script and set reason
		String reason = "";
		try {
			reason = (String) ScriptEvaluator.eval(failureReasonField, responseMap);
			addCustomFailureEvalResult(task, reason);
		} catch (Exception e) {
			logger.error(String.format("failed to evaluate failure reason for script = %s", failureReasonField), e);

			// Set the error message instead of false
			addCustomFailureEvalResult(task, e.getMessage());
		}

		processCustomFailureReasonResponse(task, response, failureReasonField, reason);
	}

	private void processCustomFailureReasonResponse(Task task, HttpResponse response, String failureReason, String reason) {
		// show full response body if the provided field for failure reason does not exist
		if (StringUtils.isEmpty(reason)) {
			//logger.error(String.format("unable to retrieve failure reason from the provided custom field, see details: field = %s, task id = %s, workflow id = %s",
					//failureReason, task.getTaskId(), task.getWorkflowInstanceId())); // log this so we can track how often this happens
			setReasonForIncompletion(response, task);
			return;
		}
		task.setReasonForIncompletion(reason);
	}

	private void addCustomFailureEvalResult(Task task, String result) {
		Map<String, Object> taskOutput = task.getOutputData();
		Map<String, Object> customFailureReason = (Map<String, Object>) taskOutput.get(CUSTOM_FAILURE_REASON_PARAMETER_NAME);
		if (isNull(customFailureReason)) {
			customFailureReason = new HashMap<>();
			taskOutput.put(CUSTOM_FAILURE_REASON_PARAMETER_NAME, customFailureReason);
		}
		customFailureReason.put("reason", result);
	}

	private void checkHttpResponseValidation(Task task, HttpResponse response) {
		Object responseParam = task.getInputData().get(RESPONSE_PARAMETER_NAME);

		// Check http_response object is present or not
		if (responseParam == null) {
			return;
		}
		Output output = om.convertValue(responseParam, Output.class);
		Validate validate = output.getValidate();

		// Check validate object is present or not
		if (validate == null) {
			return;
		}

		// Check condition object is present or not
		if (validate.getConditions() == null || validate.getConditions().isEmpty()) {
			return;
		}

		// Get the response map
		Map<String, Object> responseMap = response.asMap();

		// Go over all conditions and evaluate them
		AtomicBoolean overallStatus = new AtomicBoolean(true);
		validate.getConditions().forEach((name, condition) -> {
			try {
				Boolean success = ScriptEvaluator.evalBool(condition, responseMap);
				//logger.trace("Evaluation resulted in " + success + " for " + name + "=" + condition);

				// Failed ?
				if (!success) {

					// Add condition evaluation result into output map
					addEvalResult(task, name, success);

					// Set the over all status to false
					overallStatus.set(false);
				}
			} catch (Exception ex) {
				//logger.error("Evaluation failed for " + name + "=" + condition, ex);

				// Set the error message instead of false
				addEvalResult(task, name, ex.getMessage());

				// Set the over all status to false
				overallStatus.set(false);
			}
		});

		// If anything failed - fail the task
		if (!overallStatus.get()) {
			String overallReason = "Response validation failed";
			if (StringUtils.isNotEmpty(validate.getReason())) {
				overallReason = validate.getReason();
			}
			task.setReasonForIncompletion(overallReason);
			task.setStatus(Status.FAILED);
		}
	}
}
