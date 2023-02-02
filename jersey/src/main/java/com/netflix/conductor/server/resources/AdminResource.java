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

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.core.execution.appconfig.cache.AppConfig;
import com.netflix.conductor.dao.MetadataDAO;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.service.ExecutionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.swagger.annotations.*;
import com.netflix.conductor.core.execution.ApplicationException;
import com.netflix.conductor.core.execution.ApplicationException.Code;
import com.netflix.conductor.core.config.Configuration;
import org.apache.commons.collections.CollectionUtils;
import com.netflix.conductor.service.MetadataService;
import com.netflix.conductor.core.execution.WorkflowExecutor;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * @author Viren
 *
 */
@Api(value = "/admin", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON, tags = "Admin")
@Path("/admin")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
@Singleton
public class AdminResource {
    private static Logger logger = LoggerFactory.getLogger(AdminResource.class);
    private ExecutionService service;
    private MetadataDAO metadata;
    private Configuration config;
    private QueueDAO queue;
    private String version;
    private String buildDate;
    private boolean auth_referer_bypass;
    private MetadataService metaservice;
    private WorkflowExecutor executor;

    private AppConfig appConfig;

    @Inject
    public AdminResource(Configuration config, ExecutionService service, MetadataService metaservice,QueueDAO queue, MetadataDAO metadata, AppConfig appConfig, WorkflowExecutor executor) {
        this.config = config;
        this.service = service;
        this.metaservice = metaservice;
        this.queue = queue;
        this.metadata = metadata;
        this.appConfig = appConfig;
        this.version = "UNKNOWN";
        this.buildDate = "UNKNOWN";
        this.executor = executor;
        this.auth_referer_bypass = Boolean.parseBoolean(config.getProperty("workflow.auth.referer.bypass", "false"));

        try {

            InputStream propertiesIs = this.getClass().getClassLoader().getResourceAsStream("META-INF/conductor-core.properties");
            Properties prop = new Properties();
            prop.load(propertiesIs);
            this.version = prop.getProperty("Implementation-Version");
            this.buildDate = prop.getProperty("Build-Date");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @GET
    @Path("/config")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all the configuration parameters")
    public Map<String, Object> getAllConfig() {
        Map<String, Object> map = new HashMap<>();
        map.put("version", version);
        map.put("buildDate", buildDate);

        metadata.getConfigs().forEach(entry -> {
            map.put(entry.getLeft(), entry.getRight());
        });

        return map;
    }

    @POST
    @Path("/config/{name}")
    @Consumes({MediaType.WILDCARD})
    @ApiOperation(value = "Add the configuration parameter")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", dataType = "string", paramType = "header")})
    public void addConfig(@PathParam("name") String name, String value, @Context HttpHeaders headers) {
        if (!bypassAuth(headers)) {
            String primarRole = executor.checkUserRoles(headers);
            if (!primarRole.endsWith("admin")) {
                throw new ApplicationException(Code.UNAUTHORIZED, "User does not have access privileges");
            }
            metadata.addConfig(name, value);
        } else {
            metadata.addConfig(name, value);
        }
    }

    @PUT
    @Path("/config/{name}")
    @Consumes({MediaType.WILDCARD})
    @ApiOperation(value = "Update the configuration parameter")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", dataType = "string", paramType = "header")})
    public void updateConfig(@PathParam("name") String name,  String value, @Context HttpHeaders headers) {
        if (!bypassAuth(headers)) {
            String primarRole = executor.checkUserRoles(headers);
            if (!primarRole.endsWith("admin")) {
                throw new ApplicationException(Code.UNAUTHORIZED, "User does not have access privileges");
            }
            metadata.updateConfig(name, value);
        } else {
            metadata.updateConfig(name, value);
        }
    }

    @DELETE
    @Path("/config/{name}")
    @Consumes({MediaType.WILDCARD})
    @ApiOperation(value = "Delete the configuration parameter")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", dataType = "string", paramType = "header")})
    public void deleteConfig(@PathParam("name") String name, @Context HttpHeaders headers) {
        if (!bypassAuth(headers)) {
            String primarRole = executor.checkUserRoles(headers);
            if (!primarRole.endsWith("admin")) {
                throw new ApplicationException(Code.UNAUTHORIZED, "User does not have access privileges");
            }
            metadata.deleteConfig(name);
        } else {
            metadata.deleteConfig(name);
        }
    }

    @POST
    @Consumes({MediaType.WILDCARD})
    @ApiOperation(value = "Reload configuration parameters from the database")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", dataType = "string", paramType = "header")})
    @Path("/config")
    public void reloadAllConfig(@Context HttpHeaders headers) {
        if (!bypassAuth(headers)) {
            String primarRole = executor.checkUserRoles(headers);
            if (!primarRole.endsWith("admin")) {
                throw new ApplicationException(Code.UNAUTHORIZED, "User does not have access privileges");
            }
            service.reloadConfig();
        } else {
            service.reloadConfig();
        }
    }

    @GET
    @Path("/task/{tasktype}")
    @ApiOperation("Get the list of pending tasks for a given task type")
    @Consumes({MediaType.WILDCARD})
    public List<Task> view(@PathParam("tasktype") String taskType, @DefaultValue("0") @QueryParam("start") Integer start, @DefaultValue("100") @QueryParam("count") Integer count) throws Exception {
        List<Task> tasks = service.getPendingTasksForTaskType(taskType);
        int total = start + count;
        total = (tasks.size() > total) ? total : tasks.size();
        if (start > tasks.size()) start = tasks.size();
        return tasks.subList(start, total);
    }

    @POST
    @Path("/sweep/requeue/{workflowId}")
    @ApiOperation("Queue up all the running workflows for sweep")
    @Consumes({MediaType.WILDCARD})
    @Produces({MediaType.TEXT_PLAIN})
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", dataType = "string", paramType = "header")})
    public String requeueSweep(@PathParam("workflowId") String workflowId, @Context HttpHeaders headers) throws Exception {
        if (!bypassAuth(headers)) {
            String primarRole = executor.checkUserRoles(headers);
            if (!primarRole.endsWith("admin")) {
                throw new ApplicationException(Code.UNAUTHORIZED, "User does not have access privileges");
            }
            boolean pushed = queue.pushIfNotExists(WorkflowExecutor.deciderQueue, workflowId, config.getSweepFrequency(), 5);
            return pushed + "." + workflowId;
        } else {
            boolean pushed = queue.pushIfNotExists(WorkflowExecutor.deciderQueue, workflowId, config.getSweepFrequency(), 5);
            return pushed + "." + workflowId;
        }
    }


    @GET
    @Path("/showVars")
    @ApiOperation("Gets vars")
    @Consumes({MediaType.WILDCARD})
    public HashMap<String, String> getEnv( @QueryParam("keys") List<String> keys){
        if (CollectionUtils.isEmpty(keys)){
            return null;
        }

        HashMap<String, String> vars = new HashMap<>();
        keys.forEach(x->vars.put(x, config.getProperty(x.replaceAll("_","."), "NO_VALUE_FOUND")));
        return vars;
    }


    @GET
    @Path("/appconfig/key/{key}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiOperation(value = "Retrieves the App Config for the key ")
    public String getAppConfig(@PathParam("key") String key, @Context HttpHeaders headers) {

        if (StringUtils.isEmpty(key)){
            return null;
        }
        try {
            return appConfig.getValue(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PUT
    @Path("/appconfig/key/{key}/value/{value}")
    @ApiOperation(value = "Adds a new config value to the database")
    public void setAppConfig(@PathParam("key") String key, @PathParam("value") String value, @Context HttpHeaders headers) {
        try {
            appConfig.setValue(key, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DELETE
    @Path("/appconfig/key/{key}")
    @ApiOperation(value = "Delete the App Config for the key")
    public void deleteAppConfig(@PathParam("key") String key, @Context HttpHeaders headers) {
        try {
            appConfig.removeConfig(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @GET
    @Path("/appconfig/list")
    @ApiOperation(value = "Get the list of all application configs from the database")
    public Map<String, String> getAppConfigs(@Context HttpHeaders headers) {
        try {
            return appConfig.getConfigs();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/appconfig/refresh")
    @ApiOperation(value = "Refresh the cache with list of App Configs from the database")
    public void refreshAppConfig(@Context HttpHeaders headers) {
        try {
            appConfig.reloadProperties("");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean bypassAuth(HttpHeaders headers) {
        if (!auth_referer_bypass)
            return false;

        List<String> strings = headers.getRequestHeader("Referer");
        if (CollectionUtils.isEmpty(strings))
            return false;
        return strings.get(0).contains("/docs");
    }
}
