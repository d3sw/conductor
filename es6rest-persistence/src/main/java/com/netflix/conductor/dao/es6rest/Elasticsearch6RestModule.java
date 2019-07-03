package com.netflix.conductor.dao.es6rest;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.utils.WaitUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Elasticsearch6RestModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(Elasticsearch6RestModule.class);

    @Provides
    @Singleton
    public RestClientBuilder getBuilder(Configuration config) {
        // Initial sleep to let elasticsearch servers start first
        int initialSleep = config.getIntProperty("workflow.elasticsearch.initial.sleep.seconds", 0);
        if (initialSleep > 0) {
            Uninterruptibles.sleepUninterruptibly(initialSleep, TimeUnit.SECONDS);
        }

        // Must be in format http://host:port or https://host
        String clusterAddress = config.getProperty("workflow.elasticsearch.url", "");
        if (StringUtils.isEmpty(clusterAddress)) {
            throw new RuntimeException("No `workflow.elasticsearch.url` environment defined. Exiting");
        }

        HttpHost[] hosts = Arrays.stream(clusterAddress.split(","))
            .map(HttpHost::create)
            .toArray(HttpHost[]::new);

        int timeout = config.getIntProperty("workflow.elasticsearch.timeout.seconds", 60) * 1000;
        return RestClient.builder(hosts).setMaxRetryTimeoutMillis(timeout)
            .setRequestConfigCallback(new RequestConfigCallback() {
                @Override
                public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder requestConfigBuilder) {
                    return requestConfigBuilder.setConnectionRequestTimeout(0).setSocketTimeout(timeout)
                        .setConnectTimeout(timeout);
                }
            });
    }

    @Provides
    public RestHighLevelClient getClient(Configuration config, RestClientBuilder builder) {
        RestHighLevelClient client = new RestHighLevelClient(builder);

        int connectAttempts = config.getIntProperty("workflow.elasticsearch.connection.attempts", 60);
        int connectSleepSecs = config.getIntProperty("workflow.elasticsearch.connection.sleep.seconds", 1);
        WaitUtils.wait("elasticsearch", connectAttempts, connectSleepSecs, () -> {
            try {
                // Get cluster info
                log.debug("Cluster info " + client.info());
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });

        return client;
    }

    @Override
    protected void configure() {
    }
}
