package com.netflix.conductor.core.execution.appconfig.cache;

import com.google.inject.Inject;
import com.netflix.conductor.dao.AppConfigDAO;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Class to obtain the Application specific Configuration values.
 */
public class AppConfig {

    public static Logger logger;

    private Cache<String> appCache;

    private AppConfigDAO appConfigDAO;
    private final static String APP_CACHE = "APP_CACHE";
    private final int TTL_SECONDS = (int) TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES);

    public static final String CC_EXTRACT_SERVER = "cc_extract_server";
    public static final String CHECKSUM_SERVER = "checksum_server";
    public static final String ONE_CDN_SERVER = "one-cdn_server";

    //add default values to the list
    private static HashMap<String, String> DEFAULT = new HashMap<>();

    static {
        DEFAULT.put(CC_EXTRACT_SERVER, "http://no.service."+System.getenv()+":4646");
        DEFAULT.put(CHECKSUM_SERVER, "http://no.service."+System.getenv()+":4646");
        DEFAULT.put(ONE_CDN_SERVER, "http://no.service."+System.getenv()+":4646");
    }


    @Inject
    public AppConfig(AppConfigDAO appConfigDAO) {
        CacheManager cacheManager = CacheManager.getInstance();
        appCache = cacheManager.getCache(APP_CACHE);
        this.appConfigDAO = appConfigDAO;
        logger = LogManager.getLogger(AppConfig.class);
        logger.info("Starting archiver");
    }

    /**
     * Obtain the value for a specified key. Returns null if not found
     *
     * @param key
     * @return
     * @throws Exception
     */
    public String getValue(String key) throws Exception {
        String value;
        if ((value = appCache.get(key)) == null) {
            synchronized (AppConfig.class){
                if ((value = appCache.get(key)) == null) {
                    reloadProperties(key);
                    value = appCache.get(key);
                }
            }
        }
        logger.info("AppCache: Ask for " + key + ". Got " + value == null ? DEFAULT.get(key) : value);
        return value == null ? DEFAULT.get(key) : value;
    }

    public long getLongValue(String key) throws Exception {
        String value = getValue(key);
        return Long.parseLong(value);
    }

    public int getIntValue(String key) throws Exception {
        String value = getValue(key);
        return Integer.parseInt(value);
    }


    /**
     * Method to obtain all the configs by querying the database
     *
     * @return
     * @throws Exception
     */
    public List<Pair<String, String>> getConfigs() throws Exception {
        return appConfigDAO.getConfigs();
    }

    /**
     * Method to store a new key/Value pair.
     * This method will overwrite a value if the key already exists
     *
     * @param key
     * @param value
     * @throws Exception
     */
    public void setValue(String key, String value) throws Exception {
        appConfigDAO.setAppConfigValue(key, value);
        appCache.invalidate();
    }

    /**
     * Method to remove the configuration
     *
     * @param key
     * @throws Exception
     */
    public void removeConfig(String key) throws Exception {
        appConfigDAO.removeAppConfigValue(key);
        appCache.invalidate();
    }


    /**
     * Method to refresh the cache with the values from the database
     *
     * @param testKey
     * @throws SQLException
     */
    public synchronized void reloadProperties(String testKey) throws SQLException {
        if (appCache.get(testKey) == null) {
            appCache.invalidate();
            logger.info("AppConfig testKey " + testKey + ". Invalidating Cache ");
            List<Pair<String, String>> configValues = appConfigDAO.getConfigs();
            configValues.forEach(configValue -> appCache.put(configValue.getLeft(), StrSubstitutor.replace(configValue.getRight(), System.getenv()), TTL_SECONDS));
        }
    }


}
