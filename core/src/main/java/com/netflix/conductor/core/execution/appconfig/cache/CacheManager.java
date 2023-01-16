package com.netflix.conductor.core.execution.appconfig.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


public class CacheManager {

    private static CacheManager INSTANCE;
    private Map<String, Cache<?>> cacheMap;
    private static int DEFAULT_TTL = (int) TimeUnit.SECONDS.convert(1, TimeUnit.HOURS);

    private CacheManager() {
        cacheMap = new ConcurrentHashMap<>();
    }


    public static CacheManager getInstance() {
        if (INSTANCE == null) {
            synchronized (CacheManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CacheManager();
                }
            }
        }

        return INSTANCE;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Cache<T> getCache(String cacheName) {
        return getCache(cacheName, getTTL(cacheName));
    }

    public <T> Cache<T> getCache(String cacheName, int defaultTTL) {
        Cache cache;
        if ((cache = cacheMap.get(cacheName)) == null) {
            cache = new Cache<T>(cacheName, defaultTTL);
            cacheMap.put(cacheName, cache);
        }
        return cache;
    }


    public void expire(String cacheName) {
        Cache<?> cache;
        if ((cache = cacheMap.get(cacheName)) != null) {
            cache.expire();
        }
    }

    private int getTTL(String name) {
        try {
            return (int) TimeUnit.SECONDS.convert(1, TimeUnit.HOURS);
        } catch (Exception ex) {
            return DEFAULT_TTL;
        }
    }

}
