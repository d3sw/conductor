package com.netflix.conductor.archiver.job;

import com.netflix.conductor.archiver.config.AppConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WorkflowErrorRegistryJob extends AbstractJob {

    private static final Logger logger = LogManager.getLogger(WorkflowErrorRegistryJob.class);
    private final AppConfig config = AppConfig.getInstance();
    private ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newSingleThreadExecutor();

    public WorkflowErrorRegistryJob(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void cleanup() throws Exception {
        long retentionPeriod = System.currentTimeMillis() - Duration.ofDays(config.keepDays()).toMillis();
        logger.info(String.format("Starting workflow error registry cleanup with configured number of retention period %d, deletion will start from timestamp %s",
                config.keepDays(), new Timestamp(retentionPeriod)));

        // delete affected records
        deleteRecords();

        // wait for all threads to execute
        waitForTasksHandler();
    }

    private void deleteRecords() {
        Runnable runnable = () -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    String query = "DELETE FROM workflow_error_registry WHERE workflow_id NOT IN " +
                            "(SELECT workflow_id FROM workflow WHERE end_time < (current_date  - interval '? days')) " +
                            "and end_time < (current_date  - interval '? days')";

                    executeUpdate(connection, query, config.keepDays());
                    connection.commit();

                } catch (Throwable exception) {
                    connection.rollback();
                    throw exception;
                }
            } catch (Exception e) {
                logger.error("error running workflow error cleanup", e);
            }
        };

        // assign cleanup to the thread pool
        threadPoolExecutor.submit(runnable);
    }

    private void waitForTasksHandler() throws InterruptedException {
        while (!threadPoolExecutor.getQueue().isEmpty()) {
            Thread.sleep(1000);
        }
        threadPoolExecutor.shutdown();
        threadPoolExecutor.awaitTermination(5, TimeUnit.MINUTES);
    }

    private void executeUpdate(Connection tx, String query, int retentionPeriod) throws SQLException {
        try (PreparedStatement statement = tx.prepareStatement(query)) {
            String period = String.valueOf(retentionPeriod);
            statement.setString(1, period);
            statement.setString(2, period);
            statement.executeUpdate();
        }
    }
}
