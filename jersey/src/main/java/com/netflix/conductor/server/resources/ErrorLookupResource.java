package com.netflix.conductor.server.resources;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.run.ErrorLookup;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.dao.ErrorLookupDAO;
import com.netflix.conductor.dao.MetadataDAO;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.service.ExecutionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Api(value = "/error", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON, tags = "Error Lookup")
@Path("/error")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Singleton

public class ErrorLookupResource {
    private static Logger logger = LoggerFactory.getLogger(ErrorLookupResource.class);
    private ExecutionService service;
    private ErrorLookupDAO errorLookupDAO;
    private Configuration config;
    private String version;
    private String buildDate;

    @Inject
    public ErrorLookupResource(Configuration config, ErrorLookupDAO errorLookupDAO) {
        this.config = config;
        this.errorLookupDAO = errorLookupDAO;
        this.version = "UNKNOWN";
        this.buildDate = "UNKNOWN";

        try {
            InputStream propertiesIs = this.getClass().getClassLoader().getResourceAsStream("META-INF/conductor-core.properties");
            Properties prop = new Properties();
            prop.load(propertiesIs);
            this.version = prop.getProperty("Implementation-Version");
            this.buildDate = prop.getProperty("Build-Date");
        }catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @GET
    @Path("/list")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all the error lookups")
    public List<ErrorLookup> getErrors() {
        logger.debug("Called getErrors");
        return errorLookupDAO.getErrors();
    }

    @POST
    @Path("/lookup/{error}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Lookup a particular error")
    public List<ErrorLookup> getErrors(@PathParam("error") String error) {
        logger.debug("Called getErrors");
        return errorLookupDAO.getErrorMatching(error);
    }

    @POST
    @Path("/error")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Create a new error")
    public void addError(ErrorLookup errorLookup) {
        logger.debug("Called getErrors");
        errorLookupDAO.addError(errorLookup);
    }

    @PUT
    @Path("/error")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Create a new error")
    public void updateError(ErrorLookup errorLookup) {
        logger.debug("Called getErrors");
        errorLookupDAO.addError(errorLookup);
    }


//    @POST
//    @Path("/lookup/{error}")
//    @Consumes({ MediaType.WILDCARD })
//    @ApiOperation(value = "Lookup a particular error")
//    public void addConfig(@PathParam("name") String name, String value) {
//        metadata.addConfig(name, value);
//    }
//
//    @PUT
//    @Path("/config/{name}")
//    @Consumes({ MediaType.WILDCARD })
//    @ApiOperation(value = "Update the configuration parameter")
//    public void updateConfig(@PathParam("name") String name, String value) {
//        metadata.updateConfig(name, value);
//    }
//
//    @DELETE
//    @Path("/config/{name}")
//    @Consumes({ MediaType.WILDCARD })
//    @ApiOperation(value = "Delete the configuration parameter")
//    public void deleteConfig(@PathParam("name") String name) {
//        metadata.deleteConfig(name);
//    }
//
//    @POST
//    @Consumes({ MediaType.WILDCARD })
//    @Produces(MediaType.APPLICATION_JSON)
//    @ApiOperation(value = "Reload configuration parameters from the database")
//    @Path("/config")
//    public void reloadAllConfig() {
//        service.reloadConfig();
//    }
//
//    @GET
//    @Path("/task/{tasktype}")
//    @ApiOperation("Get the list of pending tasks for a given task type")
//    @Consumes({ MediaType.WILDCARD })
//    public List<Task> view(@PathParam("tasktype") String taskType, @DefaultValue("0") @QueryParam("start") Integer start, @DefaultValue("100") @QueryParam("count") Integer count) throws Exception {
//        List<Task> tasks = service.getPendingTasksForTaskType(taskType);
//        int total = start + count;
//        total = (tasks.size() > total) ? total : tasks.size();
//        if(start > tasks.size()) start = tasks.size();
//        return tasks.subList(start, total);
//    }
//
//    @POST
//    @Path("/sweep/requeue/{workflowId}")
//    @ApiOperation("Queue up all the running workflows for sweep")
//    @Consumes({ MediaType.WILDCARD })
//    @Produces({ MediaType.TEXT_PLAIN })
//    public String requeueSweep(@PathParam("workflowId") String workflowId) throws Exception {
//        boolean pushed = queue.pushIfNotExists(WorkflowExecutor.deciderQueue, workflowId, config.getSweepFrequency());
//        return pushed + "." + workflowId;
//    }
}
