package com.netflix.conductor.dao.es5rest;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.utils.WaitUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

public class Elasticsearch5RestModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(Elasticsearch5RestModule.class);

    @Provides
    @Singleton
    public RestHighLevelClient getClient(Configuration config) throws Exception {
        // Initial sleep to let elasticsearch servers start first
        int initialSleep = config.getIntProperty("workflow.elasticsearch.initial.sleep.seconds", 0);
        if (initialSleep > 0) {
            Uninterruptibles.sleepUninterruptibly(initialSleep, TimeUnit.SECONDS);
        }

        // Must be in format http://host:port or https://host
        String url = config.getProperty("workflow.elasticsearch.url", "");
        if (StringUtils.isEmpty(url)) {
            throw new RuntimeException("No `workflow.elasticsearch.url` environment defined. Exiting");
        }

        HttpHost httpHost = HttpHost.create(url);
        RestClientBuilder builder = RestClient.builder(httpHost);

        RestHighLevelClient client = new RestHighLevelClient(builder);

        int connectAttempts = config.getIntProperty("workflow.elasticsearch.connection.attempts", 60);
        int connectSleepSecs = config.getIntProperty("workflow.elasticsearch.connection.sleep.seconds", 1);
        WaitUtils.wait("elasticsearch", connectAttempts, connectSleepSecs, () -> {
            try {
                // Get cluster health status
                ClusterHealthRequest request = new ClusterHealthRequest();
                ClusterHealthResponse response = client.cluster().health(request, RequestOptions.DEFAULT);

                log.info("Cluster health " + response);
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
