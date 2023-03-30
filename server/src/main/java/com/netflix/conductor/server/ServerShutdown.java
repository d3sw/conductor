package com.netflix.conductor.server;

import com.netflix.conductor.contribs.http.HttpTask;
import com.netflix.conductor.aurora.AuroraMetadataDAO;
import com.netflix.conductor.aurora.AuroraQueueDAO;
import com.netflix.conductor.core.events.EventProcessor;
import com.netflix.conductor.core.execution.WorkflowSweeper;
import com.netflix.conductor.core.execution.batch.BatchSweeper;
import com.netflix.conductor.core.execution.tasks.SystemTaskWorkerCoordinator;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

@Singleton
public class ServerShutdown {
    private static final Logger logger = LoggerFactory.getLogger(ServerShutdown.class);
    private final SystemTaskWorkerCoordinator taskWorkerCoordinator;
    private final WorkflowSweeper workflowSweeper;
    private final EventProcessor eventProcessor;
    private final BatchSweeper batchSweeper;
    private final HttpTask httpTask;
    private final DataSource dataSource;
    private final AuroraMetadataDAO auroraMetadataDAO;
    private final AuroraQueueDAO auroraQueueDAO;

    @Inject
    public ServerShutdown(SystemTaskWorkerCoordinator taskWorkerCoordinator,
                          WorkflowSweeper workflowSweeper,
                          EventProcessor eventProcessor,
                          BatchSweeper batchSweeper,
                          HttpTask httpTask,
                          DataSource dataSource,
                          AuroraMetadataDAO auroraMetadataDAO,
                          AuroraQueueDAO auroraQueueDAO) {

        this.taskWorkerCoordinator = taskWorkerCoordinator;
        this.workflowSweeper = workflowSweeper;
        this.eventProcessor = eventProcessor;
        this.batchSweeper = batchSweeper;
        this.httpTask = httpTask;
        this.dataSource = dataSource;
        this.auroraMetadataDAO = auroraMetadataDAO;
        this.auroraQueueDAO = auroraQueueDAO;
    }

    public void shutdown() {
        batchSweeper.shutdown();
        eventProcessor.shutdown();
        workflowSweeper.shutdown();
        taskWorkerCoordinator.shutdown();
        httpTask.shutdown();
        auroraMetadataDAO.shutdown();
        auroraQueueDAO.shutdown();

        logger.info("Closing primary data source");
        if (dataSource instanceof HikariDataSource) {
            // close all open connections
            HikariDataSource datasource = (HikariDataSource) dataSource;
            datasource.getHikariPoolMXBean().softEvictConnections();
            while (datasource.getHikariPoolMXBean().getActiveConnections() > 0 ||
                    !auroraMetadataDAO.isTaskTerminated() ||
                    !auroraQueueDAO.isTaskTerminated()) {
                logger.debug("waiting for {} active connections to complete shutdown...",
                        datasource.getHikariPoolMXBean().getActiveConnections());
            }
            datasource.close();
        }
    }
}
