package gov.irs.directfile.models.message;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InMemoryMessageQueue implements MessageQueue {
    private final Map<String, Queue<QueueMessageEnvelope>> queues = new ConcurrentHashMap<>();
    private final AtomicLong messageCounter = new AtomicLong(0);

    @Override
    public void send(String queueName, String message, Map<String, String> attributes) {
        Queue<QueueMessageEnvelope> queue = queues.computeIfAbsent(queueName, k -> new ConcurrentLinkedQueue<>());
        String messageId = "inmem-" + messageCounter.incrementAndGet();
        String receiptHandle = "receipt-" + messageId;
        queue.add(new QueueMessageEnvelope(messageId, message, receiptHandle, attributes));
        log.debug("InMemory: sent message to queue {}: {}", queueName, messageId);
    }

    @Override
    public List<QueueMessageEnvelope> receive(String queueName, int maxMessages, int waitTimeSeconds) {
        Queue<QueueMessageEnvelope> queue = queues.computeIfAbsent(queueName, k -> new ConcurrentLinkedQueue<>());
        List<QueueMessageEnvelope> result = new ArrayList<>();
        for (int i = 0; i < maxMessages; i++) {
            QueueMessageEnvelope envelope = queue.poll();
            if (envelope == null) break;
            result.add(envelope);
        }
        log.debug("InMemory: received {} messages from queue {}", result.size(), queueName);
        return result;
    }

    @Override
    public void delete(String queueName, String receiptHandle) {
        log.debug("InMemory: deleted message {} from queue {}", receiptHandle, queueName);
        // In-memory implementation: message already removed on receive
    }

    public void clear() {
        queues.clear();
    }

    public int size(String queueName) {
        Queue<QueueMessageEnvelope> queue = queues.get(queueName);
        return queue == null ? 0 : queue.size();
    }
}
