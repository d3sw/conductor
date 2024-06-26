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
package com.netflix.conductor.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Oleksiy Lysak
 */
public interface MetricsDAO {
	boolean ping();

	Map<String, Object> getMetrics();

	/**
	 * Validates if the datasource in use is closed
	 * @return the status of the datasource
	 */
	boolean isDatasourceClosed();

	default List<String> getStuckChecksums(Long startTime, Long endTime) {return new ArrayList<>();}

}
