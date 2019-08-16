/**
 * Copyright 2016 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 *
 */
package com.netflix.conductor.contribs.queue.shotgun;

import com.bydeluxe.onemq.OneMQ;
import com.bydeluxe.onemq.OneMQClient;
import com.bydeluxe.onemq.Subscription;
import com.netflix.conductor.core.events.EventQueues;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.core.events.queue.ObservableQueue;
import com.netflix.conductor.core.events.queue.OnMessageHandler;
import com.netflix.conductor.metrics.Monitors;
import d3sw.shotgun.shotgunpb.ShotgunOuterClass;
import io.nats.client.NUID;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.NDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Oleksiy Lysak
 */
public class ShotgunQueue implements ObservableQueue {
    private static Logger logger = LoggerFactory.getLogger(ShotgunQueue.class);
    protected LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<>();
    private Duration[] publishRetryIn;
    private final String queueURI;
    private final String service;
    private final String subject;
    private final String groupId;
    private Subscription subs;
    private OneMQClient conn;
    private boolean manualAck;
    private int prefetchSize;
    private OnMessageHandler handler;

    public ShotgunQueue(String dns, String service, String queueURI, Duration[] publishRetryIn,
                        boolean manualAck, int prefetchSize, OnMessageHandler handler) {
        this.service = service;
        this.queueURI = queueURI;
        this.publishRetryIn = publishRetryIn;
        this.manualAck = manualAck;
        this.prefetchSize = prefetchSize;
        this.handler = handler;

        // If groupId specified (e.g. subject:groupId) - split to subject & groupId
        if (queueURI.contains(":")) {
            this.subject = queueURI.substring(0, queueURI.indexOf(':'));
            this.groupId = queueURI.substring(queueURI.indexOf(':') + 1);
        } else {
            this.subject = queueURI;
            this.groupId = UUID.randomUUID().toString();
        }
        logger.debug(String.format("Init queueURI=%s, subject=%s, groupId=%s, manualAck=%s, prefetchSize=%s",
            queueURI, subject, groupId, manualAck, prefetchSize));

        try {
            conn = new OneMQ();
            conn.connect(dns, null, null);
        } catch (Exception ex) {
            logger.debug("OneMQ client connect failed {}", ex.getMessage(), ex);
        }
    }

    @Override
    public Observable<Message> observe() {
        if (subs != null) {
            return null;
        }

        try {
            logger.debug(String.format("Start subscription subject=%s, groupId=%s, manualAck=%s, prefetchSize=%s",
                subject, groupId, manualAck, prefetchSize));
            subs = conn.subscribe(subject, service, groupId, manualAck, prefetchSize, this::onMessage);
        } catch (Exception ex) {
            logger.debug("Subscription failed with " + ex.getMessage() + " for queueURI " + queueURI, ex);
        }

        return null;
    }

    @Override
    public String getType() {
        return EventQueues.QueueType.shotgun.name();
    }

    @Override
    public String getName() {
        return queueURI;
    }

    @Override
    public String getURI() {
        return queueURI;
    }

    @Override
    public List<String> ack(List<Message> messages) {
        if (!manualAck) {
            return Collections.emptyList();
        }
        messages.forEach(msg -> {
            try {
                conn.ack(msg.getReceipt());
            } catch (Exception e) {
                logger.debug("ack failed with " + e.getMessage() + " for " + msg.getId(), e);
            }
        });
        return Collections.emptyList();
    }

    @Override
    public void unack(List<Message> messages) {
        if (!manualAck) {
            return;
        }
        messages.forEach(msg -> {
            try {
                conn.unack(msg.getReceipt());
            } catch (Exception e) {
                logger.debug("unack failed with " + e.getMessage() + " for " + msg.getId(), e);
            }
        });
    }

    @Override
    public void setUnackTimeout(Message message, long unackTimeout) {
    }

    @Override
    public long size() {
        return messages.size();
    }

    @Override
    public int getPrefetchSize() {
        return prefetchSize;
    }

    @Override
    public void publish(List<Message> messages) {
        messages.forEach(message -> {
            String payload = message.getPayload();
            try {
                logger.debug(String.format("Publishing to %s: %s", subject, payload));
                conn.publish(subject, payload.getBytes(), service, message.getTraceId(), publishRetryIn);
                logger.info(String.format("Published to %s: %s", subject, payload));
            } catch (Exception eo) {
                logger.debug(String.format("Publish failed for %s: %s", subject, payload), eo);
            }
        });
    }

    @Override
    public void close() {
        logger.debug("Close for " + queueURI);
        if (subs != null) {
            try {
                conn.unsubscribe(subs);
            } catch (Exception ignore) {
            }
            subs = null;
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception ignore) {
            }
            conn = null;
        }
    }

    private void onMessage(Subscription subscription, ShotgunOuterClass.Message message) {
        String uuid = UUID.randomUUID().toString();
        NDC.push("event-" + uuid);
        try {
            String payload = message.getContent().toStringUtf8();

            Message dstMsg = new Message();
            dstMsg.setId(uuid);
            dstMsg.setReceipt(message.getID());
            dstMsg.setPayload(payload);
            dstMsg.setReceived(System.currentTimeMillis());
            dstMsg.setTraceId(message.getTraceID());

            logger.info(String.format("Received message for %s/%s/%s %s=%s",
                subscription.getSubject(), subscription.getGroupID(), message.getTraceID(), dstMsg.getId(), payload));

            if (handler != null) {
                handler.apply(this, dstMsg);
            } else {
                ack(Collections.singletonList(dstMsg));
                logger.debug("No handler - ack " + dstMsg.getReceipt());
            }
        } catch (Exception ex) {
            logger.debug("onMessage failed " + ex.getMessage(), ex);
        } finally {
            NDC.remove();
        }
    }
}
