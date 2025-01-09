package com.netflix.conductor.core.execution.alerts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlertScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AlertScheduler.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AlertProcessor alertProcessor;

    @Inject
    public AlertScheduler(AlertProcessor alertProcessor) {
        this.alertProcessor = alertProcessor;
        scheduler.scheduleAtFixedRate(() -> {
            try {
                alertProcessor.processAlerts();
            } catch (Exception e) {
                logger.error("Error processing alerts", e);
            }
        }, 0, 1, TimeUnit.MINUTES);
    }


    public void stop() {
        logger.info("Stopping AlertScheduler...");
        scheduler.shutdown();
    }
}
