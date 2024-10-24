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
package com.netflix.conductor.common.run;

import com.netflix.conductor.common.metadata.Auditable;
import com.netflix.conductor.common.metadata.tasks.Task;
import org.postgresql.util.PGobject;

import java.util.*;

public class WorkflowDetails extends Auditable {

	public enum  WorkflowStatus {
		RUNNING(false, false), COMPLETED(true, true), FAILED(true, false), TIMED_OUT(true, false), TERMINATED(true, false), CANCELLED(true, false), PAUSED(false, true), RESET(true, false);

		private boolean terminal;

		private boolean successful;

		WorkflowStatus(boolean terminal, boolean successful){
			this.terminal = terminal;
			this.successful = successful;
		}

		public boolean isTerminal(){
			return terminal;
		}

		public boolean isSuccessful(){
			return successful;
		}
	}

	private long revision = 1;

	private WorkflowStatus status = WorkflowStatus.RUNNING;

	private long endTime;

	private String workflowId;

	private String parentWorkflowId;

	private String parentWorkflowTaskId;

	private List<Task> tasks = new LinkedList<>();

	private Map<String, Object> input = new HashMap<>();

	private Map<String, Object> output = new HashMap<>();;

	private Map<String, Object> attributes = new HashMap<>();;

	private String workflowType;

	private int version;

	private String correlationId;

	private String reRunFromWorkflowId;

	private String reasonForIncompletion;

	private int schemaVersion;

	private String event;

	private Map<String, String> taskToDomain = new HashMap<>();

	private List<String> workflowIds = new ArrayList<>();

	private String contextToken;

	private String contextUser;

	private String clientId;

	private Set<String> tags = new HashSet<>();

	private int restartCount;

	private int rerunCount;

	private String cancelledBy;

	private String traceId;

	private boolean resetTags;

	private int jobPriority = 5;

    private Map<String, Object> variables = new HashMap<>();

	private Map<String, String> metaConfigs = new HashMap<>();

	private Map<String, String> priorityConfig = new HashMap<>();

	private PGobject jsonDataWorkflowIds;


	public WorkflowDetails(){
		
	}
	/**
	 * @return the status
	 */
	public WorkflowStatus getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(WorkflowStatus status) {
		this.status = status;
	}

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return getCreateTime();
	}

	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.setCreateTime(startTime);
	}

	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * @param endTime the endTime to set
	 */
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	/**
	 * @return the duration of the workflow
	 */
	public long getDuration() {
		return getEndTime() - getStartTime();
	}

	/**
	 * @return the workflowId
	 */
	public String getWorkflowId() {
		return workflowId;
	}
	/**
	 * @param workflowId the workflowId to set
	 */
	public void setWorkflowId(String workflowId) {
		this.workflowId = workflowId;
	}
	/**
	 * @return the tasks which are scheduled, in progress or completed.
	 */
	public List<Task> getTasks() {
		return tasks;
	}
	/**
	 * @param tasks the tasks to set
	 */
	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}
	
	/**
	 * @return the input
	 */
	public Map<String, Object> getInput() {
		return input;
	}
	/**
	 * @param input the input to set
	 */
	public void setInput(Map<String, Object> input) {
		this.input = input;
	}
	/**
	 * @return the task to domain map
	 */
	public Map<String, String> getTaskToDomain() {
		return taskToDomain;
	}
	/**
	 * @param taskToDomain the task to domain map
	 */
	public void setTaskToDomain(Map<String, String> taskToDomain) {
		this.taskToDomain = taskToDomain;
	}
	/**
	 * @return the output
	 */
	public Map<String, Object> getOutput() {
		return output;
	}
	/**
	 * @param output the output to set
	 */
	public void setOutput(Map<String, Object> output) {
		this.output = output;
	}
	
	/**
	 * 
	 * @return The correlation id used when starting the workflow
	 */
	public String getCorrelationId() {
		return correlationId;
	}
	
	/**
	 * 
	 * @param correlationId the correlation id
	 */
	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}
	
	/**
	 * 
	 * @return Workflow Type / Definition
	 */
	public String getWorkflowType() {
		return workflowType;
	}
	
	/**
	 * 
	 * @param workflowType Workflow type
	 */
	public void setWorkflowType(String workflowType) {
		this.workflowType = workflowType;
	}
	
	
	/**
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}
	/**
	 * @param version the version to set
	 */
	public void setVersion(int version) {
		this.version = version;
	}
	
	public String getReRunFromWorkflowId() {
		return reRunFromWorkflowId;
	}
	
	public void setReRunFromWorkflowId(String reRunFromWorkflowId) {
		this.reRunFromWorkflowId = reRunFromWorkflowId;
	}
	
	public String getReasonForIncompletion() {
		return reasonForIncompletion;
	}
	
	public void setReasonForIncompletion(String reasonForIncompletion) {
		this.reasonForIncompletion = reasonForIncompletion;
	}
	
	/**
	 * @return the parentWorkflowId
	 */
	public String getParentWorkflowId() {
		return parentWorkflowId;
	}
	/**
	 * @param parentWorkflowId the parentWorkflowId to set
	 */
	public void setParentWorkflowId(String parentWorkflowId) {
		this.parentWorkflowId = parentWorkflowId;
	}

	/**
	 * @return whether this workflow is a sub-workflow.
	 */
	public boolean isSubWorkflow() {
		final String parentId = getParentWorkflowId();

		return parentId != null ? !parentId.isEmpty() : false;
	}

	/**
	 * @return the parentWorkflowTaskId
	 */
	public String getParentWorkflowTaskId() {
		return parentWorkflowTaskId;
	}
	/**
	 * @param parentWorkflowTaskId the parentWorkflowTaskId to set
	 */
	public void setParentWorkflowTaskId(String parentWorkflowTaskId) {
		this.parentWorkflowTaskId = parentWorkflowTaskId;
	}
	/**
	 * @return the schemaVersion Version of the schema for the workflow definition
	 */
	public int getSchemaVersion() {
		return schemaVersion;
	}
	/**
	 * @param schemaVersion the schemaVersion to set
	 */
	public void setSchemaVersion(int schemaVersion) {
		this.schemaVersion = schemaVersion;
	}
	
	/**
	 * 
	 * @return Name of the event that started the workflow
	 */
	public String getEvent() {
		return event;
	}
	
	/**
	 * 
	 * @param event Name of the event that started the workflow
	 */
	public void setEvent(String event) {
		this.event = event;
	}

	public List<String> getWorkflowIds() {
		return workflowIds;
	}

	public void setWorkflowIds(List<String> workflowIds) {
		this.workflowIds = workflowIds;
	}

	public String getContextToken() {
		return contextToken;
	}

	public void setContextToken(String contextToken) {
		this.contextToken = contextToken;
	}

	public String getContextUser() {
		return contextUser;
	}

	public void setContextUser(String contextUser) {
		this.contextUser = contextUser;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public int getRestartCount() {
		return restartCount;
	}

	public void setRestartCount(int restartCount) {
		this.restartCount = restartCount;
	}

	public void incRestartCount() {
		this.restartCount++;
	}

	public String getCancelledBy() {
		return cancelledBy;
	}

	public void setCancelledBy(String cancelledBy) {
		this.cancelledBy = cancelledBy;
	}

	public int getRerunCount() {
		return rerunCount;
	}

	public void setRerunCount(int rerunCount) {
		this.rerunCount = rerunCount;
	}

	public void incRerunCount() {
		this.rerunCount++;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public int getJobPriority() {
		return jobPriority;
	}

	public void setJobPriority(int jobPriority) {
		this.jobPriority = jobPriority;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public boolean getResetTags() {
		return resetTags;
	}

	public void setResetTags(boolean resetTags) {
		this.resetTags = resetTags;
	}

	public PGobject getJsonDataWorkflowIds() {
		return jsonDataWorkflowIds;
	}

	public void setJsonDataWorkflowIds(PGobject jsonDataWorkflowIds) {
		this.jsonDataWorkflowIds = jsonDataWorkflowIds;
	}

	@Override
	public String toString() {
		return workflowType + "." + version + "/" + workflowId + "." + status; 
	}

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

	public Map<String, String> getMetaConfigs() {
		return metaConfigs;
	}

	public void setMetaConfigs(Map<String, String> metaConfigs) {
		this.metaConfigs = metaConfigs;
	}

	public Map<String, String> getPriorityConfig() {
		return priorityConfig;
	}

	public void setPriorityConfig(Map<String, String> priorityConfig) {
		this.priorityConfig = priorityConfig;
	}
}
