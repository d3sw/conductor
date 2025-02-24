package com.netflix.conductor.archiver.job;

import com.netflix.conductor.archiver.config.AppConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

public class AlertsJob extends AbstractJob {
	private static final Logger logger = LogManager.getLogger(AlertsJob.class);

	public AlertsJob(HikariDataSource dataSource) {
		super(dataSource);
	}

	@Override
	public void cleanup() {
		logger.info("Starting alerts cleanup job");
		try {
			AppConfig config = AppConfig.getInstance();
			int batchSize = config.batchSize();

			// Calculate the start of the current day
			Timestamp startOfToday = new Timestamp(System.currentTimeMillis() - TimeUnit.MILLISECONDS.toMillis(System.currentTimeMillis() % TimeUnit.DAYS.toMillis(1)));
			logger.info("Deleting records earlier than " + startOfToday + ", batch size = " + batchSize);

			int deleted = 0;
			List<Long> ids = fetchIds("SELECT id FROM alerts WHERE timestamp < ? LIMIT ?", startOfToday, batchSize);
			while (isNotEmpty(ids)) {
				deleted += deleteByIds("alerts", ids);
				logger.debug("Alerts cleanup job deleted " + deleted + " records");

				ids = fetchIds("SELECT id FROM alerts WHERE timestamp < ? LIMIT ?", startOfToday, batchSize);
			}
			logger.info("Finished alerts cleanup job. Total records deleted: " + deleted);
		} catch (Exception ex) {
			logger.error("Alerts cleanup job failed: " + ex.getMessage(), ex);
		}
	}
}
