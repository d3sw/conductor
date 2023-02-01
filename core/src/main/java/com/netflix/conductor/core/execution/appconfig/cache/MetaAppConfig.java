package com.netflix.conductor.core.execution.appconfig.cache;

import com.google.inject.Inject;
import com.netflix.conductor.dao.MetadataDAO;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Class to obtain the Application specific Configuration values.
 */
public class MetaAppConfig {

    private Cache<String> appCache;
    private MetadataDAO metadataDAO;
    private final static String APP_CACHE = "APP_CACHE";
    private final int TTL_SECONDS = (int) TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES);

    public static final String CC_EXTRACT_SERVER = "cc_extract_server";
    public static final String CHECKSUM_SERVER = "checksum_server";
    public static final String ONE_CDN_SERVER = "one-cdn_server";

    //add default values to the list
    private static HashMap<String, String> DEFAULT = new HashMap<>();

    static {
        DEFAULT.put(CC_EXTRACT_SERVER, "http://one-batch.service." + System.getenv("TLD"));
        DEFAULT.put(CHECKSUM_SERVER, "http://one-batch.service." + System.getenv("TLD"));
        DEFAULT.put(ONE_CDN_SERVER, "http://one-batch.service." + System.getenv("TLD"));
    }


    @Inject
    public MetaAppConfig(MetadataDAO metadataDAO) {
        CacheManager cacheManager = CacheManager.getInstance();
        appCache = cacheManager.getCache(APP_CACHE);
        this.metadataDAO = metadataDAO;
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
            synchronized (MetaAppConfig.class){
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
     * Method to refresh the cache with the values from the database
     *
     * @param testKey
     * @throws SQLException
     */
    public synchronized void reloadProperties(String testKey) throws SQLException {
        if (appCache.get(testKey) == null) {
            appCache.invalidate();
            Pair<String, String> configValue = metadataDAO.getConfigsByName(testKey);
            appCache.put(configValue.getLeft(), StrSubstitutor.replace(configValue.getRight(), System.getenv()), TTL_SECONDS);
        }
    }


}
