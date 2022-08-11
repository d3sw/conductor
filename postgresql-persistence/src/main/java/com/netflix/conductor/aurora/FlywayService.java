package com.netflix.conductor.aurora;

import com.netflix.conductor.core.config.Configuration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FlywayService {

    public static final String PATH_TO_MIGRATIONS = "classpath:db/migrations";
    private static final Logger logger = LoggerFactory.getLogger(FlywayService.class);

    public static void migrate(Configuration config) {
        String db = config.getProperty("aurora.db", null);
        String host = config.getProperty("aurora.host", null);
        String port = config.getProperty("aurora.port", "5432");
        String user = config.getProperty("aurora.user", null);
        String pwd = config.getProperty("aurora.password", null);
        String baselineVersion = config.getProperty("FLYWAY_BASELINE_VERSION", "1");

        String url = String.format("jdbc:postgresql://%s:%s/%s", host, port, db);
        logger.info("Start Flyway on " + url);

        Flyway flyway = Flyway.configure()
                .dataSource(url, user, pwd)
                .locations(PATH_TO_MIGRATIONS)
                .baselineOnMigrate(true)
                .baselineVersion(baselineVersion)
                .loggers("slf4j2", "log4j2")
                .load();
        MigrateResult result = flyway.migrate();
        logger.info("Flyway result.success " + result.success);
        if ( !result.success){
            if (result.warnings != null && result.warnings.size() > 0){
                result.warnings.stream().forEach(x-> logger.info("Warning: " + x));
            }
        }

        logger.info("Flyway complete ");

    }
}
