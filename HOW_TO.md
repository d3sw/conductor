# Local Shotgun setup for testing
Shotgun is a MQ service which handles messages in Conductor.
The main class with MQ configuration is
**com.netflix.conductor.contribs.queue.shotgun.SharedShotgunQueue**
Queue name in conductor has following pattern :
***{queue.name}:{group.id}*** 
For example: **deluxe.one-packaging.package.progress:deluxe.conductor.queue.packaging.progress.1.1**
Where  **deluxe.one-packaging.package.progress** is a subject or queue url and
**deluxe.conductor.queue.packaging.progress.1.1** is a group id which defines to what consumer group consumer belongs to
To listen the messages from the same queue but in different consumer group use custom groupId value
For example:
```java
public class SharedShotgunQueue implements ObservableQueue {
    public SharedShotgunQueue(OneMQClient conn, String service, String queueURI, Duration[] publishRetryIn,
                              boolean manualAck, int prefetchSize, OnMessageHandler handler) {
        this.conn = conn;
        this.service = service;
        this.queueURI = queueURI;
        this.publishRetryIn = publishRetryIn;
        this.manualAck = manualAck;
        this.prefetchSize = prefetchSize;
        this.handler = handler;
        this.subject = "deluxe.one-packaging.package.progress";
        this.groupId = "local";
        logger.debug(String.format("Init queueURI=%s, subject=%s, groupId=%s, manualAck=%s, prefetchSize=%s",
                queueURI, subject, groupId, manualAck, prefetchSize));
    }
}
```
With this config the local conductor application will be listening to all the messages from
**deluxe.one-packaging.package.progress** topic in parallel to other consumers (in separate consumer group).

        
