package com.netflix.conductor.core.execution.alerts;

import com.netflix.conductor.dao.ExecutionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;


public class AlertProcessor  {
    private static final Logger logger = LoggerFactory.getLogger(AlertProcessor.class);

    private ExecutionDAO edao;

    @Inject
    public AlertProcessor(ExecutionDAO edao) {
        this.edao = edao;
    }

    public void processAlerts() {
        logger.info("Fetching grouped alerts from alerts table...");
        Map<Integer, Integer> groupedAlerts = edao.getGroupedAlerts();

        groupedAlerts.forEach((alertLookupId, alertCount) -> {
            Integer registryAlertCount = edao.getAlertCountFromRegistry(alertLookupId);
            if (registryAlertCount != null && alertCount > registryAlertCount) {
                logger.info("Alert threshold exceeded for lookup ID: {}. Count: {}, Threshold: {}",
                        alertLookupId, alertCount, registryAlertCount);
                notifyService(alertLookupId, alertCount);
            }
        });
    }

    private void notifyService(Integer alertLookupId, int alertCount) {
        logger.info("Notifying service for alertLookupId: {}, alertCount: {}", alertLookupId, alertCount);
        // Add logic to call the notify service
    }
}

