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
package com.netflix.conductor.server.resources;

import com.netflix.conductor.common.metadata.tasks.PollData;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskExecLog;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.service.ExecutionService;
import datadog.trace.api.Trace;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.log4j.NDC;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.Map.Entry;

/**
 * 
 * @author visingh
 *
 */
@Api(value="/tasks", produces=MediaType.APPLICATION_JSON, consumes=MediaType.APPLICATION_JSON, tags="Task Management")
@Path("/tasks")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Singleton
public class TaskResource {

	private ExecutionService taskService;

	private QueueDAO queues;

	@Inject
	public TaskResource(ExecutionService taskService, QueueDAO queues) {
		this.taskService = taskService;
		this.queues = queues;
	}

	@GET
	@Path("/poll/{tasktype}")
	@Consumes({ MediaType.WILDCARD })
	@ApiOperation("Poll for a task of a certain type")
	@Trace(operationName = "poll", resourceName = "task")
	public Task poll(@PathParam("tasktype") String taskType, @QueryParam("workerid") String workerId, @QueryParam("domain") String domain) throws Exception {
		List<Task> tasks = taskService.poll(taskType, workerId, domain, 1, 100);
		if(tasks.isEmpty()) {
			return null;
		}
		return tasks.get(0);
	}
	
	@GET
	@Path("/poll/batch/{tasktype}")
	@ApiOperation("batch Poll for a task of a certain type")
	@Consumes({ MediaType.WILDCARD })
	@Trace(operationName = "batch.poll", resourceName = "task")
	public List<Task> batchPoll(
			@PathParam("tasktype") String taskType, 
			@QueryParam("workerid") String workerId,
			@QueryParam("domain") String domain,
			@DefaultValue("1") @QueryParam("count") Integer count,
			@DefaultValue("100") @QueryParam("timeout") Integer timeout
			
			) throws Exception {
		return taskService.poll(taskType, workerId, domain, count, timeout);
	}

	@GET
	@Path("/in_progress/{tasktype}")
	@ApiOperation("Get in progress tasks.  The results are paginated.")
	@Consumes({ MediaType.WILDCARD })
	public List<Task> getTasks(@PathParam("tasktype") String taskType, @QueryParam("startKey") String startKey,
			@QueryParam("count") @DefaultValue("100") Integer count) throws Exception {
		return taskService.getTasks(taskType, startKey, count);
	}

	@GET
	@Path("/in_progress/{workflowId}/{taskRefName}")
	@ApiOperation("Get in progress task for a given workflow id.")
	@Consumes({ MediaType.WILDCARD })
	public Task getPendingTaskForWorkflow(@PathParam("workflowId") String workflowId, @PathParam("taskRefName") String taskReferenceName)
			throws Exception {
		return taskService.getPendingTaskForWorkflow(taskReferenceName, workflowId);
	}

	@POST
	@ApiOperation("Update a task")
	@Trace(operationName = "update", resourceName = "task")
	public String updateTask(TaskResult task) throws Exception {
		NDC.push("rest-update-task-"+ UUID.randomUUID().toString());
		try {
			taskService.updateTask(task);
		} finally {
			NDC.remove();
		}
		return "\"" + task.getTaskId() + "\"";
	}

	@POST
	@Path("/{taskId}/ack")
	@ApiOperation("Ack Task is recieved")
	@Consumes({ MediaType.WILDCARD })
	@Trace(operationName = "ack", resourceName = "task")
	public String ack(@PathParam("taskId") String taskId, @QueryParam("workerid") String workerId) throws Exception {
		return "" + taskService.ackTaskRecieved(taskId, workerId);
	}
	
	@POST
	@Path("/{taskId}/log")
	@ApiOperation("Log Task Execution Details")
	public void log(@PathParam("taskId") String taskId, String log) throws Exception {
		taskService.log(taskId, log);		
	}
	
	@GET
	@Path("/{taskId}/log")
	@ApiOperation("Get Task Execution Logs")
	public List<TaskExecLog> getTaskLogs(@PathParam("taskId") String taskId) throws Exception {
		return taskService.getTaskLogs(taskId);		
	}

	@GET
	@Path("/{taskId}")
	@ApiOperation("Get task by Id")
	@Consumes({ MediaType.WILDCARD })
	public Task getTask(@PathParam("taskId") String taskId) throws Exception {
		return taskService.getTask(taskId);
	}

	@DELETE
	@Path("/queue/{taskType}/{taskId}")
	@ApiOperation("Remove Task from a Task type queue")
	@Consumes({ MediaType.WILDCARD })
	public void remvoeTaskFromQueue(@PathParam("taskType") String taskType, @PathParam("taskId") String taskId) throws Exception {
		taskService.removeTaskfromQueue(taskType, taskId);
	}

	@GET
	@Path("/queue/sizes")
	@ApiOperation("Get Task type queue sizes")
	@Consumes({ MediaType.WILDCARD })
	public Map<String, Integer> size(@QueryParam("taskType") List<String> taskTypes) throws Exception {
		return taskService.getTaskQueueSizes(taskTypes);
	}

	@GET
	@Path("/queue/all/verbose")
	@ApiOperation("Get the details about each queue")
	@Consumes({ MediaType.WILDCARD })
	public Map<String, Map<String, Map<String, Long>>> allVerbose() throws Exception {
		return queues.queuesDetailVerbose();
	}

	@GET
	@Path("/queue/all")
	@ApiOperation("Get the details about each queue")
	@Consumes({ MediaType.WILDCARD })
	public Map<String, Long> all() throws Exception {
		Map<String, Long> all = queues.queuesDetail();
		Set<Entry<String, Long>> entries = all.entrySet();
		Set<Entry<String, Long>> sorted = new TreeSet<>(new Comparator<Entry<String, Long>>() {

			@Override
			public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		});
		sorted.addAll(entries);
		LinkedHashMap<String, Long> sortedMap = new LinkedHashMap<>();
		sorted.stream().forEach(e -> sortedMap.put(e.getKey(), e.getValue()));
		return sortedMap;
	}

	@GET
	@Path("/queue/polldata")
	@ApiOperation("Get the last poll data for a given task type")
	@Consumes({ MediaType.WILDCARD })
	public List<PollData> getPollData(@QueryParam("taskType") String taskType) throws Exception {
		return taskService.getPollData(taskType);
	}
	

	@GET
	@Path("/queue/polldata/all")
	@ApiOperation("Get the last poll data for a given task type")
	@Consumes({ MediaType.WILDCARD })
	public List<PollData> getAllPollData() throws Exception {
		return taskService.getAllPollData();
	}

	@POST
	@Path("/queue/requeue")
	@ApiOperation("Requeue pending tasks for all the running workflows")
	@Consumes({ MediaType.WILDCARD })
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public String requeue() throws Exception {
		return "" + taskService.requeuePendingTasks();
	}
	
	@POST
	@Path("/queue/requeue/{taskType}")
	@ApiOperation("Requeue pending tasks")
	@Consumes({ MediaType.WILDCARD })
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public String requeue(@PathParam("taskType") String taskType) throws Exception {
		return "" + taskService.requeuePendingTasks(taskType);
	}
	
}
