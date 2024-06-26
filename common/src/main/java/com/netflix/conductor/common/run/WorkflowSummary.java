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
package com.netflix.conductor.common.run;

import com.netflix.conductor.common.run.Workflow.WorkflowStatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * @author Viren
 *
 */
public class WorkflowSummary {

	/**
	 * The time should be stored as GMT
	 */
	private static final TimeZone gmt = TimeZone.getTimeZone("GMT");
	
	private String workflowType;
	
	private int version;
	
	private String workflowId;
	
	private String correlationId;
	
	private String startTime;
	
	private String updateTime;
	
	private String endTime;
	
	private WorkflowStatus status;
	
	private String input;
	
	private String output;
	
	private String reasonForIncompletion;
	
	private long executionTime;
	
	private String event;

	private List<String> workflowIds = new ArrayList<>();

	private String variables;

	private String metaConfigs;

	private String priorityConfigs;

	public WorkflowSummary() {
		
	}
	public WorkflowSummary(Workflow workflow) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    	sdf.setTimeZone(gmt);
    	
		this.workflowType = workflow.getWorkflowType();
		this.version = workflow.getVersion();
		this.workflowId = workflow.getWorkflowId();
		this.correlationId = workflow.getCorrelationId();
		if(workflow.getCreateTime() != null){
			this.startTime = sdf.format(new Date(workflow.getCreateTime()));
		}
		if(workflow.getEndTime() > 0){
			this.endTime = sdf.format(new Date(workflow.getEndTime()));
		}
		if(workflow.getUpdateTime() != null){
			this.updateTime = sdf.format(new Date(workflow.getUpdateTime()));
		}
		this.status = workflow.getStatus();
		this.input = workflow.getInput().toString();
		this.output = workflow.getOutput().toString();
		this.reasonForIncompletion = workflow.getReasonForIncompletion();
		if(workflow.getEndTime() > 0){
			this.executionTime = workflow.getEndTime() - workflow.getStartTime();
		}
		this.event = workflow.getEvent();
		this.workflowIds = workflow.getWorkflowIds();
		this.variables = workflow.getVariables().toString();
		this.metaConfigs = workflow.getMetaConfigs().toString();
		this.priorityConfigs = workflow.getPriorityConfig().toString();
	}

	/**
	 * @return the workflowType
	 */
	public String getWorkflowType() {
		return workflowType;
	}

	/**
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * @return the workflowId
	 */
	public String getWorkflowId() {
		return workflowId;
	}

	/**
	 * @return the correlationId
	 */
	public String getCorrelationId() {
		return correlationId;
	}

	/**
	 * @return the startTime
	 */
	public String getStartTime() {
		return startTime;
	}

	/**
	 * @return the endTime
	 */
	public String getEndTime() {
		return endTime;
	}

	/**
	 * @return the status
	 */
	public WorkflowStatus getStatus() {
		return status;
	}

	/**
	 * @return the input
	 */
	public String getInput() {
		return input;
	}
	
	/**
	 * 
	 * @return the output
	 */
	public String getOutput() {
		return output;
	}

	/**
	 * @return the reasonForIncompletion
	 */
	public String getReasonForIncompletion() {
		return reasonForIncompletion;
	}

	/**
	 * 
	 * @return the executionTime
	 */
	public long getExecutionTime(){
		return executionTime;
	}

	/**
	 * @return the updateTime
	 */
	public String getUpdateTime() {
		return updateTime;
	}
	
	/**
	 * 
	 * @return The event
	 */
	public String getEvent() {
		return event;
	}
	
	/**
	 * 
	 * @param event The event
	 */
	public void setEvent(String event) {
		this.event = event;
	}

	/**
	 * @return List of parent workflow ids
	 */
	public List<String> getWorkflowIds() {
		return workflowIds;
	}

	/**
	 * @param workflowIds List of parent workflow ids
	 */
	public void setWorkflowIds(List<String> workflowIds) {
		this.workflowIds = workflowIds;
	}

	public String getVariables() {
		return variables;
	}

	public void setVariables(String variables) {
		this.variables = variables;
	}

	public String getMetaConfigs() {
		return metaConfigs;
	}

	public void setMetaConfigs(String metaConfigs) {
		this.metaConfigs = metaConfigs;
	}

	public String getPriorityConfigs() {
		return priorityConfigs;
	}

	public void setPriorityConfigs(String priorityConfigs) {
		this.priorityConfigs = priorityConfigs;
	}
}
