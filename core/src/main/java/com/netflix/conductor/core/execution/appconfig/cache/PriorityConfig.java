package com.netflix.conductor.core.execution.appconfig.cache;

import com.google.inject.Inject;
import com.netflix.conductor.core.utils.PriorityLookup;
import com.netflix.conductor.dao.PriorityLookupDAO;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Class to obtain the Application specific Configuration values.
 */
public class PriorityConfig {
    public static Logger logger;

    private Cache<List<PriorityLookup>> appCache;
    private PriorityLookupDAO priorityLookupDAO;
    private final static String PRIORITY_CACHE = "PRIORITY_CACHE";
    private static final String CACHE_REF_KEY = "__PRIORITY_REF_KEY__";
    private final int TTL_SECONDS = (int) TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES);


    @Inject
    public PriorityConfig(PriorityLookupDAO priorityLookupDAO) {
        CacheManager cacheManager = CacheManager.getInstance();
        appCache = cacheManager.getCache(PRIORITY_CACHE);
        this.priorityLookupDAO = priorityLookupDAO;
        try {
            initialize();
        } catch (Exception e) {
            logger.error("Unable to load Priority Config ", e);
            throw new RuntimeException(e);
        }
        logger = LogManager.getLogger(PriorityConfig.class);
        logger.info("Initialized PriorityConfig");
    }

    /**
     * Initialize the cache
     *
     * @return
     * @throws Exception
     */
    private void initialize() throws Exception {
        synchronized (AppConfig.class) {
            reloadProperties();
        }
    }


    /**
     * Method to obtain all the configs by querying the database
     *
     * @return
     * @throws Exception
     */
    public List<PriorityLookup> getPriorityConfigs() throws Exception {
        if (appCache.get(CACHE_REF_KEY) == null) {
            synchronized (AppConfig.class) {
                if ((appCache.get(CACHE_REF_KEY)) == null) {
                    reloadProperties();
                }
            }
        }
        return appCache.get(CACHE_REF_KEY);
    }

    /**
     * Method to refresh the cache with the values from the database
     *
     * @throws SQLException
     */
    public synchronized void reloadProperties() throws SQLException {
        if (appCache.get(CACHE_REF_KEY) == null) {
            appCache.invalidate();
            List<PriorityLookup> configValue = priorityLookupDAO.getAllPriorities();
            appCache.put(CACHE_REF_KEY, configValue, TTL_SECONDS);
        }
    }
}
