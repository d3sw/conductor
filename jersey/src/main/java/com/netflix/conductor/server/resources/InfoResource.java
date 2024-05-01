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
package com.netflix.conductor.server.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.netflix.conductor.core.DNSLookup;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.dao.MetadataDAO;
import com.netflix.conductor.dao.MetricsDAO;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import com.sun.jersey.client.apache4.config.DefaultApacheHttpClient4Config;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Oleksiy Lysak
 *
 */
@Singleton
@Path("/v1")
@Api(value = "/v1", produces = MediaType.APPLICATION_JSON, tags = "Status Info")
@Produces({MediaType.APPLICATION_JSON})
public class InfoResource {
	private static final Logger logger = LoggerFactory.getLogger(InfoResource.class);
	private final MetricsDAO metricsDAO;
	private final Configuration config;
	private String fullVersion;
	private MetadataDAO metadata;
	public static final String INITIALIZER_VERSION_NAME = "initializer_app_version";


	@Inject
	public InfoResource(Configuration config, MetricsDAO metricsDAO, MetadataDAO metadata) {
		this.config = config;
		this.metricsDAO = metricsDAO;
		this.metadata = metadata;
		try {
			InputStream propertiesIs = this.getClass().getClassLoader().getResourceAsStream("META-INF/conductor-core.properties");
			Properties prop = new Properties();
			prop.load(propertiesIs);
			String version = prop.getProperty("Implementation-Version");
			String change = prop.getProperty("Change");
			fullVersion = config.getProperty("APP.VERSION", version + "-" + change);

		} catch (Exception e) {
			logger.error("Failed to read conductor-core.properties" + e.getMessage(), e);
		}
	}

	@GET
	@Path("/status")
	@ApiOperation(value = "Get the status")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> status(){
		Map<String, Object> versionMap = new HashMap<>();
		versionMap.put("version", fullVersion);
		Map<String, String> configMap = metadata.getConfigByName(INITIALIZER_VERSION_NAME);
		versionMap.put("initializerVersion", configMap != null ? configMap.get(INITIALIZER_VERSION_NAME) : "");
		try {
			String composerServiceUrl = "workflowcomposer.service." + Optional.ofNullable(System.getenv("TLD")).orElse("default");
			versionMap.put("composerVersion", getComposerVersion(composerServiceUrl));
		} catch (Exception e) {
			logger.error("Failed to retrieve composer version", e);
			versionMap.put("composerVersion", "Error retrieving version");
		}
		return versionMap;
	}

	private String getComposerVersion(String service) {
		UriBuilder uriBuilder = UriBuilder.fromUri(DNSLookup.lookup(service)).path("/v1/status");
		try {
			Client client = ApacheHttpClient4.create(new DefaultApacheHttpClient4Config());
			WebResource webResource = client.resource(uriBuilder.build());
			ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON)
					.get(ClientResponse.class);

			if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode() || !response.hasEntity()) {
				return "";
			}

			if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
				String json = response.getEntity(String.class);
				ObjectMapper objectMapper = new ObjectMapper();
				Map<String, Object> versionMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
				});
				return (String) versionMap.get("version");
			} else {
				throw new RuntimeException("Failed to fetch version: HTTP error code " + response.getStatus());
			}
		} catch (Exception e) {
			logger.error("Error while fetching composer version info", e);
			throw new RuntimeException("Error fetching version", e);
		}
	}

	@GET
	@Path("/health")
	@ApiOperation(value = "Get the health status")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> health() throws IOException {

		if(isMaintenanceMode()){
			return Collections.singletonMap("is_ping_okay", true);
		}else{
			boolean status = false;

			try {
				// bypass datastore ping if it is already closed
				status = metricsDAO.isDatasourceClosed() ? true : metricsDAO.ping();
			} catch (Exception e) {
				logger.error("Db health check failed: " + e.getMessage(), e);
				throw e;
			}

			return Collections.singletonMap("is_ping_okay", status);
		}
	}

	@GET
	@Path("/stuckChecksums")
	@ApiOperation(value = "Get the list of checksum jobs that are potentially stuck")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> stuckChecksums(){
		try {
			Calendar threeDaysAgo = Calendar.getInstance();
			threeDaysAgo.add(Calendar.DATE, -3);

			Calendar thirtyMinsAgo = Calendar.getInstance();
			thirtyMinsAgo.add(Calendar.MINUTE, -30);

			return metricsDAO.getStuckChecksums(threeDaysAgo.getTimeInMillis(), thirtyMinsAgo.getTimeInMillis());
		} catch (Exception e) {
			logger.error("Error while fetching checksum info " + e.getMessage(), e);
			throw e;
		}
	}

	@GET
	@Path("/dependencies")
	@ApiOperation(value = "Get the dependencies")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> dependencies() {
		List<Object> endpoints = new ArrayList<>();
		endpoints.add(config.getProperty("conductor.auth.url", ""));
		endpoints.add("events.service." + config.getProperty("TLD", "local"));
		endpoints.add("vault.service." + config.getProperty("TLD", "local"));

		List<Map<String, Object>> dependencies = new ArrayList<>();
		dependencies.add(ImmutableMap.<String, Object>builder()
			.put("name", "auth")
			.put("version", "v1")
			.put("scheme", "https")
			.put("external", false)
			.build());
		dependencies.add(ImmutableMap.<String, Object>builder()
			.put("name", "vault")
			.put("version", "v1")
			.put("scheme", "http")
			.put("external", false)
			.build());
		dependencies.add(ImmutableMap.<String, Object>builder()
			.put("name", "shotgun")
			.put("version", "v1")
			.put("scheme", "amq")
			.put("external", false)
			.build());
		dependencies.add(ImmutableMap.<String, Object>builder()
			.put("name", "*")
			.put("version", "v1")
			.put("scheme", "http")
			.put("external", false)
			.build());

		Map<String, Object> result = new HashMap<>();
		result.put("version", fullVersion);
		result.put("endpoints", endpoints);
		result.put("dependencies", dependencies);
		return result;
	}

	@GET
	@Path("/metrics")
	@ApiOperation(value = "Get the metrics")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> metrics() {
		return metricsDAO.getMetrics();
	}

	private boolean isMaintenanceMode(){
		return "true".equalsIgnoreCase(config.getProperty("MAINTENANCE_MODE", "false"));
	}
}
