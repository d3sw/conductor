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
package com.netflix.conductor.contribs;

import com.google.inject.AbstractModule;
import com.netflix.conductor.contribs.status.SimpleStatusHandler;
import com.netflix.conductor.contribs.status.UpdatePackagingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sangeeta Nadgir
 */
public class StatusModule extends AbstractModule {
	private static Logger logger = LoggerFactory.getLogger(StatusModule.class);

	@Override
	protected void configure() {
		bind(SimpleStatusHandler.class).asEagerSingleton();
		bind(UpdatePackagingStatus.class).asEagerSingleton();
		logger.debug("Status Module configured ...");
	}
}
