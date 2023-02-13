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
    private final int TTL_SECONDS = (int) TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES);


    @Inject
    public PriorityConfig(PriorityLookupDAO priorityLookupDAO) {
        CacheManager cacheManager = CacheManager.getInstance();
        appCache = cacheManager.getCache(PRIORITY_CACHE);
        this.priorityLookupDAO = priorityLookupDAO;
        logger = LogManager.getLogger(PriorityConfig.class);
        logger.info("Initialized PriorityConfig");
    }


    /**
     * Obtain the value for a specified key. Returns null if not found
     *
     * @param priority
     * @return
     * @throws Exception
     */
    public List<PriorityLookup> getValue(Integer priority) throws Exception {
        List<PriorityLookup> value;
        if ((value = appCache.get(Integer.toString(priority))) == null) {
            synchronized (PriorityConfig.class) {
                if ((value = appCache.get(Integer.toString(priority))) == null) {
                    reloadProperties(priority);
                    value = appCache.get(Integer.toString(priority));
                }
            }
        }
        return value;
    }


    /**
     * Method to refresh the cache with the values from the database
     *
     * @param priority
     * @throws SQLException
     */
    public synchronized void reloadProperties(Integer priority) throws SQLException {
        if (appCache.get(Integer.toString(priority)) == null) {
            appCache.invalidate();
            List<PriorityLookup> configValue = priorityLookupDAO.getPriority(priority);
            appCache.put(Integer.toString(priority), configValue, TTL_SECONDS);
        }
    }
}
