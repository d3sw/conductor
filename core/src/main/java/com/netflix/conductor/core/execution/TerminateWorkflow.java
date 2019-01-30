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
package com.netflix.conductor.core.execution;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.run.Workflow.WorkflowStatus;

 
/**
 * 
 * @author Viren
 *
 */
@SuppressWarnings("serial")
public class TerminateWorkflow extends RuntimeException {
	
	WorkflowStatus workflowStatus;
	
	boolean cancelled;

	Task task;

	TerminateWorkflow(String reason){
		this(reason, WorkflowStatus.FAILED);
	}
	
	TerminateWorkflow(String reason, WorkflowStatus workflowStatus){
		this(reason, workflowStatus, null);
	}
	
	TerminateWorkflow(String reason, WorkflowStatus workflowStatus, Task task){
		super(reason);
		this.workflowStatus = workflowStatus;
		this.task = task;
	}

	TerminateWorkflow(String reason, WorkflowStatus workflowStatus, Task task, boolean cancelled){
		this(reason, workflowStatus, task);
		this.cancelled = cancelled;
	}
}