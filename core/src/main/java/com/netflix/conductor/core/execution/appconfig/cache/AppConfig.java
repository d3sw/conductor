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
    private static final String CACHE_REF_KEY = "__REF_KEY__" ;
    private final int TTL_SECONDS = (int) TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES);

    public static final String CC_EXTRACT_SERVER = "cc_extract_server";
    public static final String CHECKSUM_SERVER = "checksum_server";
    public static final String ONE_CDN_SERVER = "one-cdn_server";

    //add default values to the list
    private static HashMap<String, String> DEFAULT = new HashMap<>();

    static {
        DEFAULT.put(CC_EXTRACT_SERVER, "http://nomad.service." + System.getenv("TLD") + ":4646");
        DEFAULT.put(CHECKSUM_SERVER, "http://nomad.service." + System.getenv("TLD") + ":4646");
        DEFAULT.put(ONE_CDN_SERVER, "http://nomad.service." + System.getenv("TLD") + ":4646");
    }


    @Inject
    public AppConfig(AppConfigDAO appConfigDAO) {
        CacheManager cacheManager = CacheManager.getInstance();
        appCache = cacheManager.getCache(APP_CACHE);
        this.appConfigDAO = appConfigDAO;
        try {
            initialize();
        } catch (Exception e) {
            logger.error("Unable to load App Config ", e);
            throw new RuntimeException(e);
        }
        logger = LogManager.getLogger(AppConfig.class);
        logger.info("Initialized AppConfig");
    }

    /**
     * Initialize the cache
     *
     * @return
     * @throws Exception
     */
    private void initialize() throws Exception {
        synchronized (AppConfig.class){
            reloadProperties("");
        }
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
    public Map<String, String> getConfigs() throws Exception {
        if ( getValue(CACHE_REF_KEY) == null){
            synchronized (AppConfig.class){
                if ((appCache.get(CACHE_REF_KEY)) == null) {
                    reloadProperties(CACHE_REF_KEY);
                }
            }
        }

        return appCache.getCurrentCache();
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
            Map<String, String> configValues = appConfigDAO.getConfigs();
            configValues.entrySet().forEach(configValue -> appCache.put(configValue.getKey(), StrSubstitutor.replace(configValue.getValue(), System.getenv()), TTL_SECONDS));
            appCache.put(CACHE_REF_KEY, CACHE_REF_KEY, TTL_SECONDS);
        }
    }


}
