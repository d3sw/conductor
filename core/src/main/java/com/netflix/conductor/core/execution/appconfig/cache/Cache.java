package com.netflix.conductor.core.execution.appconfig.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Cache<T> {

    private int TTL;
    private String name;
    private Map<String, CacheItem<T>> cache;

    private Cache() {
        this("", 60 * 60);
    }

    public Cache(String name, int TTL) {
        cache = new ConcurrentHashMap<String, CacheItem<T>>();
        this.name = name;
        this.TTL = TTL;
    }


    public void put(String key, T value) {
        cache.put(key, new CacheItem<T>(value, TTL));
    }

    public void put(String key, T value, int TTL_seconds) {
        cache.put(key, new CacheItem<T>(value, TTL_seconds));
    }

    public T get(String key) {
        T returnData = null;

        CacheItem<T> cachedItem;

        if ((cachedItem = cache.get(key)) != null) {
            returnData = cachedItem.getData();

            if (isExpired(cachedItem)) {
                cache.remove(key);
                returnData = null;
            }
        }
        return returnData;
    }

    private boolean isExpired(CacheItem<T> cachedItem) {
        long nowSeconds = (System.nanoTime() / 1_000_000_000);
        return isExpired(nowSeconds, cachedItem);
    }

    private boolean isExpired(long nowSeconds, CacheItem<T> cachedItem) {
        return nowSeconds > cachedItem.getExpirationTimeInSeconds();
    }

    public void invalidate() {
        cache.clear();
    }

    public void expire() {
        long nowSeconds = (System.nanoTime() / 1_000_000_000);
        cache.entrySet().removeIf(entry -> isExpired(nowSeconds, entry.getValue()));
    }


}




