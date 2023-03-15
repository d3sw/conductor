/**
 * Copyright 2017 Netflix, Inc.
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
package com.netflix.conductor.server;

import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * @author Viren
 * Entry point for the server
 */
public class Main {

	static {
		// Workaround to send java util logging to log4j
		java.util.logging.LogManager.getLogManager().reset();
		org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger();
		org.slf4j.bridge.SLF4JBridgeHandler.install();
		java.util.logging.Logger.getLogger("global").setLevel(java.util.logging.Level.FINEST);
	}

	public static void main(String[] args) throws Exception {
		if (args.length > 0) {
			String propertyFile = args[0];
			FileInputStream propFile = new FileInputStream(propertyFile);
			Properties props = new Properties(System.getProperties());
			props.load(propFile);
			System.setProperties(props);
		}

		if (args.length == 2) {
			PropertyConfigurator.configure(new FileInputStream(new File(args[1])));
		}

		ConductorConfig config = new ConductorConfig();
		ConductorServer server = new ConductorServer(config);

		server.start(config.getIntProperty("port", 8080), true);
	}
}