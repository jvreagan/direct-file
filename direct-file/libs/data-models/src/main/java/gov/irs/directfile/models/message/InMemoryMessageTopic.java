package gov.irs.directfile.models.message;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InMemoryMessageTopic implements MessageTopic {
    private final Map<String, List<BiConsumer<String, Map<String, String>>>> subscribers = new ConcurrentHashMap<>();
    private final List<PublishedMessage> publishedMessages = new CopyOnWriteArrayList<>();

    public record PublishedMessage(String topicId, String message, Map<String, String> attributes) {}

    @Override
    public void publish(String topicId, String message) {
        publish(topicId, message, Map.of());
    }

    @Override
    public void publish(String topicId, String message, Map<String, String> attributes) {
        publishedMessages.add(new PublishedMessage(topicId, message, attributes));
        List<BiConsumer<String, Map<String, String>>> topicSubscribers = subscribers.get(topicId);
        if (topicSubscribers != null) {
            topicSubscribers.forEach(subscriber -> subscriber.accept(message, attributes));
        }
        log.debug("InMemory: published message to topic {}", topicId);
    }

    public void subscribe(String topicId, BiConsumer<String, Map<String, String>> handler) {
        subscribers.computeIfAbsent(topicId, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public List<PublishedMessage> getPublishedMessages() {
        return Collections.unmodifiableList(publishedMessages);
    }

    public List<PublishedMessage> getPublishedMessages(String topicId) {
        return publishedMessages.stream()
                .filter(m -> m.topicId().equals(topicId))
                .toList();
    }

    public void clear() {
        publishedMessages.clear();
        subscribers.clear();
    }
}
