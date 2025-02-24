package com.netflix.conductor.core.execution.alerts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.run.AlertRegistry;
import com.netflix.conductor.core.DNSLookup;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.dao.ExecutionDAO;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class AlertProcessor  {
    private static final Logger logger = LoggerFactory.getLogger(AlertProcessor.class);

    private ExecutionDAO edao;

    private final String notficationUrl;

    private final String environment;

    public static final String PROPERTY_URL = "conductor.notify.url";


    private static final ObjectMapper mapper = new ObjectMapper();


    @Inject
    public AlertProcessor(ExecutionDAO edao, Configuration config) {
        this.edao = edao;
        notficationUrl = config.getProperty(PROPERTY_URL, null);
        environment = config.getProperty("TLD", "local");
    }

    public void processAlerts() {
        Map<Integer, Integer> groupedAlerts = edao.getGroupedAlerts();

        groupedAlerts.forEach((alertLookupId, alertCount) -> {
            AlertRegistry alertRegistry = edao.getAlertRegistryFromLookupId(alertLookupId);
            if (alertRegistry != null && alertCount > alertRegistry.getAlertCount()) {
                logger.info("Alert threshold exceeded for lookup ID: {}. Count: {}, Threshold: {}",
                        alertLookupId, alertCount, alertRegistry.getAlertCount());
                try {
                    notifyService(alertLookupId, alertCount, alertRegistry.getGeneralMessage());
                    edao.markAlertsAsProcessed(alertLookupId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void notifyService(Integer alertLookupId, int alertCount, String message) throws Exception {
        String serviceDiscoveryQuery = "notify.service."+ Optional.ofNullable(System.getenv("TLD")).orElse("default");
        String uri = "/v1/notify";
        String method = "POST";
        String contentType = MediaType.APPLICATION_JSON;

        Map<String, Object> body = new HashMap<>();
        body.put("message", "");
        Map<String, Object> outputs = new HashMap<>();

        Map<String, Object> slack = new HashMap<>();
        slack.put("username", "conductor:"+environment);
        slack.put("channel", "#conductor-"+environment);
        slack.put("text", message);
        outputs.put("slack", slack);

        body.put("outputs", outputs);

        String response = post(
                uri,
                method,
                contentType,
                body,
                logger,
                serviceDiscoveryQuery
        );
    }



    public  String post(
            String uri,
            String method,
            String contentType,
            Object body,
            Logger logger,
            String serviceDiscoveryQuery) throws Exception {

        String hostAndPort = DNSLookup.lookup(serviceDiscoveryQuery);
        String url = (StringUtils.isEmpty(hostAndPort) ?  this.notficationUrl : hostAndPort) + uri;

        Client client = Client.create();
        WebResource.Builder webResource = client.resource(url)
                .type(contentType != null ? contentType : MediaType.APPLICATION_JSON);

        ClientResponse response;
        if ("POST".equalsIgnoreCase(method)) {
            response = webResource.post(ClientResponse.class, mapper.writeValueAsString(body));
        } else if ("PUT".equalsIgnoreCase(method)) {
            response = webResource.put(ClientResponse.class, mapper.writeValueAsString(body));
        } else if ("DELETE".equalsIgnoreCase(method)) {
            response = webResource.delete(ClientResponse.class);
        } else {
            response = webResource.get(ClientResponse.class);
        }

        String entity = response.getEntity(String.class);
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            return entity;
        } else {
            logger.error("HTTP Error {}: {}", response.getStatus(), entity);
            throw new Exception("HTTP Error " + response.getStatus() + ": " + entity);
        }
    }
}

