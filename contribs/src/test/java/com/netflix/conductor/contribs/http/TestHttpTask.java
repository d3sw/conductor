/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.auth.AuthManager;
import com.netflix.conductor.auth.ForeignAuthManager;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.Task.Status;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask.Type;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.execution.DeciderService;
import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.dao.MetadataDAO;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static com.netflix.conductor.contribs.http.GenericHttpTask.CUSTOM_FAILURE_REASON_PARAMETER_NAME;
import static com.netflix.conductor.contribs.http.GenericHttpTask.RESPONSE_PARAMETER_NAME;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Viren
 *
 */
@SuppressWarnings("unchecked")
public class TestHttpTask {

	private static final String ERROR_RESPONSE = "Something went wrong!";
	
	private static final String TEXT_RESPONSE = "Text Response";
	
	private static final double NUM_RESPONSE = 42.42d;
	
	private static String JSON_RESPONSE;
	
	private HttpTask httpTask;
	
	private WorkflowExecutor executor = mock(WorkflowExecutor.class);

	private AuthManager authManager = mock(AuthManager.class);

	private ForeignAuthManager foreignAuthManager = mock(ForeignAuthManager.class);

	private Workflow workflow = new Workflow();
	
	private static Server server;
	
	private static ObjectMapper om = new ObjectMapper();
	
	@BeforeClass
	public static void init() throws Exception {
		
		Map<String, Object> map = new HashMap<>();
		map.put("key", "value1");
		map.put("num", 42);
		JSON_RESPONSE = om.writeValueAsString(map);
		
		server = new Server(7009);
		ServletContextHandler servletContextHandler = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
		servletContextHandler.setHandler(new EchoHandler());
		server.start();
	}
	
	@AfterClass
	public static void cleanup() {
		if(server != null) {
			try {
				server.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Before
	public void setup() {
		Configuration config = mock(Configuration.class);
		when(config.getServerId()).thenReturn("test_server_id");
		RestClientManager rcm = new RestClientManager();
		httpTask = new HttpTask(rcm, config, new ObjectMapper(), authManager, foreignAuthManager,null);
	}
	
	@Test
	public void testPost() throws Exception {

		Task task = new Task();
		Input input = new Input();
		input.setUri("http://localhost:7009/post");
		Map<String, Object> body = new HashMap<>();
		body.put("input_key1", "value1");
		body.put("input_key2", 45.3d);
		input.setBody(body);
		input.setMethod("POST");
		task.getInputData().put(HttpTask.REQUEST_PARAMETER_NAME, input);
		
		httpTask.start(workflow, task, executor);
		assertEquals(task.getReasonForIncompletion(), Task.Status.COMPLETED, task.getStatus());
		Map<String, Object> hr = (Map<String, Object>) task.getOutputData().get("response");
		Object response = hr.get("body");
		assertEquals(Task.Status.COMPLETED, task.getStatus());
		assertTrue("response is: " + response, response instanceof Map);
		Map<String, Object> map = (Map<String, Object>) response;
		Set<String> inputKeys = body.keySet();
		Set<String> responseKeys = map.keySet();
		inputKeys.containsAll(responseKeys);
		responseKeys.containsAll(inputKeys);
	}

	@Test
	public void testPostWithCustomFailureReason() throws Exception {
		// Arrange: set up task, mock handler input
		Task task = new Task();
		Input input = new Input();
		input.setUri("http://localhost:7009/failure_reason");
		String expectedFailureReason = "some failure reason message";
		Map<String, Object> body = new HashMap<>();
		body.put("input_key1", "value1");
		body.put("input_key2", 45.3d);
		input.setBody(body);
		input.setMethod("POST");
		Map<String, String> failureReason = new HashMap<>();
		failureReason.put("field", "$.body.failureReason");
		Map<String, Object> httpResponse = new HashMap<>();
		httpResponse.put(CUSTOM_FAILURE_REASON_PARAMETER_NAME, failureReason);
		task.getInputData().put(HttpTask.REQUEST_PARAMETER_NAME, input);

		// sample JSON overview of the data structure represented here
		// "http_response": {
		//   "response_failure_reason": {
		//       "field": "$.body.failureReason"
		//   }
		//},
		task.getInputData().put(RESPONSE_PARAMETER_NAME, httpResponse);

		// Act: invoke an HTTP task
		httpTask.start(workflow, task, executor);

		// Assert: Ensure that the in-completion failure reason for this task is as configured
		assertEquals(task.getReasonForIncompletion(), Status.FAILED, task.getStatus());
		assertEquals(expectedFailureReason, task.getReasonForIncompletion());
	}

	@Test
	public void testPostWithCustomFailureReason_whenBadConfiguration() throws Exception {
		// Arrange: set up task, mock handler input
		Task task = new Task();
		Input input = new Input();
		input.setUri("http://localhost:7009/failure_reason");
		Map<String, Object> body = new HashMap<>();
		body.put("input_key1", "value1");
		body.put("input_key2", 45.3d);
		input.setBody(body);
		input.setMethod("POST");
		Map<String, String> failureReason = new HashMap<>();
		failureReason.put("field", "$.body.badField"); // using a bad field
		Map<String, Object> httpResponse = new HashMap<>();
		httpResponse.put(CUSTOM_FAILURE_REASON_PARAMETER_NAME, failureReason);
		task.getInputData().put(HttpTask.REQUEST_PARAMETER_NAME, input);
		task.getInputData().put(RESPONSE_PARAMETER_NAME, httpResponse);

		// Act: invoke an HTTP task
		httpTask.start(workflow, task, executor);

		// Assert: Ensure that the in-completion failure reason for this task is the full response body because of the bad configuration
		assertEquals(task.getReasonForIncompletion(), Status.FAILED, task.getStatus());
		assertEquals("{input_key1=input_key1, input_key2=input_key2, failureReason=some failure reason message}", task.getReasonForIncompletion());
	}
	

	@Test
	public void testPostNoContent() throws Exception {

		Task task = new Task();
		Input input = new Input();
		input.setUri("http://localhost:7009/post2");
		Map<String, Object> body = new HashMap<>();
		body.put("input_key1", "value1");
		body.put("input_key2", 45.3d);
		input.setBody(body);
		input.setMethod("POST");
		task.getInputData().put(HttpTask.REQUEST_PARAMETER_NAME, input);
		
		httpTask.start(workflow, task, executor);
		assertEquals(task.getReasonForIncompletion(), Task.Status.COMPLETED, task.getStatus());
		Map<String, Object> hr = (Map<String, Object>) task.getOutputData().get("response");
		Object response = hr.get("body");
		assertEquals(Task.Status.COMPLETED, task.getStatus());
		assertNull("response is: " + response, response);
	}
	
	@Test
	public void testFailure() throws Exception {

		Task task = new Task();
		Input input = new Input();
		input.setUri("http://localhost:7009/failure");
		input.setMethod("GET");
		task.getInputData().put(HttpTask.REQUEST_PARAMETER_NAME, input);
		
		httpTask.start(workflow, task, executor);
		assertEquals("Task output: " + task.getOutputData(), Task.Status.FAILED, task.getStatus());
		assertEquals(ERROR_RESPONSE, task.getReasonForIncompletion());
		
		task.setStatus(Status.SCHEDULED);
		task.getInputData().remove(HttpTask.REQUEST_PARAMETER_NAME);
		httpTask.start(workflow, task, executor);
		assertEquals(Task.Status.FAILED, task.getStatus());
		assertEquals(HttpTask.MISSING_REQUEST, task.getReasonForIncompletion());
	}
	
	@Test
	public void testTextGET() throws Exception {

		Task task = new Task();
		Input input = new Input();
		input.setUri("http://localhost:7009/text");
		input.setMethod("GET");
		task.getInputData().put(HttpTask.REQUEST_PARAMETER_NAME, input);
		
		httpTask.start(workflow, task, executor);
		Map<String, Object> hr = (Map<String, Object>) task.getOutputData().get("response");
		Object response = hr.get("body");
		assertEquals(Task.Status.COMPLETED, task.getStatus());
		assertEquals(TEXT_RESPONSE, response);	
	}
	
	@Test
	public void testNumberGET() throws Exception {

		Task task = new Task();
		Input input = new Input();
		input.setUri("http://localhost:7009/numeric");
		input.setMethod("GET");
		task.getInputData().put(HttpTask.REQUEST_PARAMETER_NAME, input);
		
		httpTask.start(workflow, task, executor);
		Map<String, Object> hr = (Map<String, Object>) task.getOutputData().get("response");
		Object response = hr.get("body");
		assertEquals(Task.Status.COMPLETED, task.getStatus());
		assertEquals(NUM_RESPONSE, response);
		assertTrue(response instanceof Number);
	}
	
	@Test
	public void testJsonGET() throws Exception {

		Task task = new Task();
		Input input = new Input();
		input.setUri("http://localhost:7009/json");
		input.setMethod("GET");
		task.getInputData().put(HttpTask.REQUEST_PARAMETER_NAME, input);
		
		httpTask.start(workflow, task, executor);
		Map<String, Object> hr = (Map<String, Object>) task.getOutputData().get("response");
		Object response = hr.get("body");
		assertEquals(Task.Status.COMPLETED, task.getStatus());
		assertTrue(response instanceof Map);
		Map<String, Object> map = (Map<String, Object>) response;
		assertEquals(JSON_RESPONSE, om.writeValueAsString(map));
	}

	@Test
	public void testExecute() throws Exception {

		Task task = new Task();
		Input input = new Input();
		input.setUri("http://localhost:7009/json");
		input.setMethod("GET");
		task.getInputData().put(HttpTask.REQUEST_PARAMETER_NAME, input);
		task.setStatus(Status.SCHEDULED);
		task.setScheduledTime(0);
		boolean executed = httpTask.execute(workflow, task, executor);
		assertFalse(executed);

	}
	
	@Test
	public void testOptional() throws Exception {
 		Task task = new Task();
 		Input input = new Input();
 		input.setUri("http://localhost:7009/failure");
 		input.setMethod("GET");
 		task.getInputData().put(HttpTask.REQUEST_PARAMETER_NAME, input);
 		
 		httpTask.start(workflow, task, executor);
 		assertEquals("Task output: " + task.getOutputData(), Task.Status.FAILED, task.getStatus());
 		assertEquals(ERROR_RESPONSE, task.getReasonForIncompletion());
 		assertTrue(!task.getStatus().isSuccessful());
 		
 		task.setStatus(Status.SCHEDULED);
 		task.getInputData().remove(HttpTask.REQUEST_PARAMETER_NAME);
 		task.setReferenceTaskName("t1");
 		httpTask.start(workflow, task, executor);
 		assertEquals(Task.Status.FAILED, task.getStatus());
 		assertEquals(HttpTask.MISSING_REQUEST, task.getReasonForIncompletion());
 		assertTrue(!task.getStatus().isSuccessful());
 		
 		Workflow workflow = new Workflow();
 		workflow.getTasks().add(task);
 		
 		WorkflowDef def = new WorkflowDef();
 		WorkflowTask wft = new WorkflowTask();
 		wft.setOptional(true);
 		wft.setName("HTTP");
 		wft.setWorkflowTaskType(Type.USER_DEFINED);
 		wft.setTaskReferenceName("t1");
		def.getTasks().add(wft);
 		MetadataDAO metadata = mock(MetadataDAO.class);
 		new DeciderService(metadata, null, mock(Configuration.class)).decide(workflow, def);
 		
 		System.out.println(workflow.getTasks());
 		System.out.println(workflow.getStatus());
 	}

 	@Test
	public void testOAuth() throws Exception {
		Task task = new Task();
		Input input = new Input();
		input.setUri("http://localhost:7009/oauth");
		input.setMethod("POST");
		input.setOauthConsumerKey("someKey");
		input.setOauthConsumerSecret("someSecret");
		task.getInputData().put(HttpTask.REQUEST_PARAMETER_NAME, input);

		httpTask.start(workflow, task, executor);

		Map<String, Object> response = (Map<String, Object>) task.getOutputData().get("response");
		Map<String, String> body = (Map<String, String>) response.get("body");

		assertEquals("someKey", body.get("oauth_consumer_key"));
		assertTrue("Should have OAuth nonce", body.containsKey("oauth_nonce"));
		assertTrue("Should have OAuth signature", body.containsKey("oauth_signature"));
		assertTrue("Should have OAuth signature method", body.containsKey("oauth_signature_method"));
		assertTrue("Should have OAuth oauth_timestamp", body.containsKey("oauth_timestamp"));
		assertTrue("Should have OAuth oauth_version", body.containsKey("oauth_version"));
		
		assertEquals("Task output: " + task.getOutputData(), Status.COMPLETED, task.getStatus());
	}

	@Test
	public void testStatusMapping() throws Exception {

		Task task = new Task();
		Input input = new Input();
		input.setUri("http://localhost:7009/bad_request");
		Map<String, Object> body = new HashMap<>();
		body.put("input_key1", "value1");
		body.put("input_key2", 45.3d);
		input.setBody(body);
		input.setMethod("POST");
		task.getInputData().put(HttpTask.REQUEST_PARAMETER_NAME, input);
		task.getInputData().put(HttpTask.STATUS_MAPPING_PARAMETER_NAME, Collections.singletonMap("400", "COMPLETED"));

		httpTask.start(workflow, task, executor);
		assertEquals(task.getReasonForIncompletion(), Task.Status.COMPLETED, task.getStatus());
		Map<String, Object> hr = (Map<String, Object>) task.getOutputData().get("response");
		Object response = hr.get("body");
		assertEquals(Task.Status.COMPLETED, task.getStatus());
		assertTrue("response is: " + response, response instanceof Map);
		Map<String, Object> map = (Map<String, Object>) response;
		Set<String> inputKeys = body.keySet();
		Set<String> responseKeys = map.keySet();
		inputKeys.containsAll(responseKeys);
		responseKeys.containsAll(inputKeys);
	}
	
	private static class EchoHandler extends AbstractHandler {

		private TypeReference<Map<String, Object>> mapOfObj = new TypeReference<Map<String,Object>>() {};
		
		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			if(request.getMethod().equals("GET") && request.getRequestURI().equals("/text")) {
				PrintWriter writer = response.getWriter();
				writer.print(TEXT_RESPONSE);
				writer.flush();
				writer.close();
			} else if(request.getMethod().equals("GET") && request.getRequestURI().equals("/json")) {
				response.addHeader("Content-Type", "application/json");
				PrintWriter writer = response.getWriter();
				writer.print(JSON_RESPONSE);
				writer.flush();
				writer.close();
			} else if(request.getMethod().equals("GET") && request.getRequestURI().equals("/failure")) {
				response.addHeader("Content-Type", "text/plain");
				response.setStatus(500);
				PrintWriter writer = response.getWriter();
				writer.print(ERROR_RESPONSE);
				writer.flush();
				writer.close();
			} else if(request.getMethod().equals("POST") && request.getRequestURI().equals("/failure_reason")) {
				response.addHeader("Content-Type", "application/json");
				response.setStatus(404);
				BufferedReader reader = request.getReader();
				Map<String, Object> input = om.readValue(reader, mapOfObj);
				Set<String> keys = input.keySet();
				for(String key : keys) {
					input.put(key, key);
				}
				input.put("failureReason", "some failure reason message");
				PrintWriter writer = response.getWriter();
				writer.print(om.writeValueAsString(input));
				writer.flush();
				writer.close();
			} else if(request.getMethod().equals("POST") && request.getRequestURI().equals("/post")) {
				response.addHeader("Content-Type", "application/json");
				BufferedReader reader = request.getReader();
				Map<String, Object> input = om.readValue(reader, mapOfObj);
				Set<String> keys = input.keySet();
				for(String key : keys) {
					input.put(key, key);					
				}
				PrintWriter writer = response.getWriter();
				writer.print(om.writeValueAsString(input));
				writer.flush();
				writer.close();
			} else if(request.getMethod().equals("POST") && request.getRequestURI().equals("/post2")) {
				response.addHeader("Content-Type", "application/json");
				response.setStatus(204);
				BufferedReader reader = request.getReader();
				Map<String, Object> input = om.readValue(reader, mapOfObj);
				Set<String> keys = input.keySet();
				System.out.println(keys);
				response.getWriter().close();
			} else if(request.getMethod().equals("POST") && request.getRequestURI().equals("/bad_request")) {
				response.addHeader("Content-Type", "application/json");
				response.setStatus(400);
				BufferedReader reader = request.getReader();
				Map<String, Object> input = om.readValue(reader, mapOfObj);
				Set<String> keys = input.keySet();
				for(String key : keys) {
					input.put(key, key);
				}
				PrintWriter writer = response.getWriter();
				writer.print(om.writeValueAsString(input));
				writer.flush();
				writer.close();
			} else if(request.getMethod().equals("GET") && request.getRequestURI().equals("/numeric")) {
				PrintWriter writer = response.getWriter();
				writer.print(NUM_RESPONSE);
				writer.flush();
				writer.close();
			} else if(request.getMethod().equals("POST") && request.getRequestURI().equals("/oauth")) {
				//echo back oauth parameters generated in the Authorization header in the response
				Map<String, String> params = parseOauthParameters(request);
				response.addHeader("Content-Type", "application/json");
				PrintWriter writer = response.getWriter();
				writer.print(om.writeValueAsString(params));
				writer.flush();
				writer.close();
			}
		}

		private Map<String, String> parseOauthParameters(HttpServletRequest request) {
			String paramString = request.getHeader("Authorization").replaceAll("^OAuth (.*)", "$1");
			return Arrays.stream(paramString.split("\\s*,\\s*"))
				.map(pair -> pair.split("="))
				.collect(Collectors.toMap(o -> o[0], o -> o[1].replaceAll("\"","")));
		}
	}
}
