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
package com.netflix.conductor.core.config;

import com.google.inject.AbstractModule;
import com.netflix.conductor.core.events.ActionProcessor;
import com.netflix.conductor.core.events.EventProcessor;
import com.netflix.conductor.core.events.queue.dyno.DynoEventQueueProvider;
import com.netflix.conductor.core.execution.WorkflowSweeper;
import com.netflix.conductor.core.execution.appconfig.cache.AppConfig;
import com.netflix.conductor.core.execution.appconfig.cache.PriorityConfig;
import com.netflix.conductor.core.execution.batch.BatchSweeper;
import com.netflix.conductor.core.execution.batch.SherlockBatchProcessor;
import com.netflix.conductor.core.execution.tasks.*;


/**
 * @author Viren
 *
 */
public class CoreModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(DynoEventQueueProvider.class).asEagerSingleton();
		bind(ActionProcessor.class).asEagerSingleton();
		bind(EventProcessor.class).asEagerSingleton();		
		bind(WorkflowSweeper.class).asEagerSingleton();
		bind(SystemTaskWorkerCoordinator.class).asEagerSingleton();
		bind(SubWorkflow.class).asEagerSingleton();
		bind(Wait.class).asEagerSingleton();
		bind(Event.class).asEagerSingleton();
		bind(Fail.class).asEagerSingleton();
		bind(Output.class).asEagerSingleton();
		bind(UpdateTask.class).asEagerSingleton();
		bind(SherlockBatchProcessor.class).asEagerSingleton();
		bind(BatchSweeper.class).asEagerSingleton();
		bind(Batch.class).asEagerSingleton();
		bind(Terminate.class).asEagerSingleton();
		bind(Lambda.class).asEagerSingleton();
		bind(GetTaskData.class).asEagerSingleton();
		bind(GetWorkflowData.class).asEagerSingleton();
		bind(GetTaskStatus.class).asEagerSingleton();
		bind(GetWorkflowStatus.class).asEagerSingleton();
		bind(GetConfig.class).asEagerSingleton();
		bind(ErrorLookupTask.class).asEagerSingleton();
		bind(PriorityLookupTask.class).asEagerSingleton();
		bind(SetVariable.class).asEagerSingleton();
		bind(AppConfig.class).asEagerSingleton();
		bind(PriorityConfig.class).asEagerSingleton();
	}
	
}
