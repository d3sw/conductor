package com.netflix.conductor.core.execution.appconfig.cache;

public class CacheItem<T> {

    private T data = null;
    private long expirationTimeInSeconds = 0;

    public CacheItem(T data, int TTL_seconds) {
        this.data = data;
        expirationTimeInSeconds = (System.nanoTime() / 1_000_000_000) + TTL_seconds;
    }

    /**
     * @return the ttl
     */
    public long getExpirationTimeInSeconds() {
        return expirationTimeInSeconds;
    }

    /**
     * @return the data
     */
    public T getData() {
        return data;
    }

}