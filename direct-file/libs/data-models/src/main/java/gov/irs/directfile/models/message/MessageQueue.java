package gov.irs.directfile.models.message;

import java.util.List;
import java.util.Map;

public interface MessageQueue {
    void send(String queueName, String message, Map<String, String> attributes);

    default void send(String queueName, String message) {
        send(queueName, message, Map.of());
    }

    List<QueueMessageEnvelope> receive(String queueName, int maxMessages, int waitTimeSeconds);

    void delete(String queueName, String receiptHandle);
}
