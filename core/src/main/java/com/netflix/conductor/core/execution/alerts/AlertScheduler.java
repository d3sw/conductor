package com.netflix.conductor.core.execution.alerts;

import com.netflix.conductor.core.config.Configuration;
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
    private final Configuration config;

    @Inject
    public AlertScheduler(AlertProcessor alertProcessor, Configuration config) {
        this.config = config;
        int alertInitDelay = config.getIntProperty("alert.init.delay", 0);
        int alertFrequency = config.getIntProperty("alert.frequency", 10);
        this.alertProcessor = alertProcessor;
        scheduler.scheduleAtFixedRate(() -> {
            try {
                alertProcessor.processAlerts();
            } catch (Exception e) {
                logger.error("Error processing alerts", e);
            }
        }, alertInitDelay, alertFrequency, TimeUnit.MINUTES);
    }


    public void stop() {
        logger.info("Stopping AlertScheduler...");
        scheduler.shutdown();
    }
}
