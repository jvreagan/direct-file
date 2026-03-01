package gov.irs.directfile.models.message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryMessageTopicTest {
    private InMemoryMessageTopic topic;

    @BeforeEach
    void setUp() {
        topic = new InMemoryMessageTopic();
    }

    @Test
    void givenMessage_whenPublish_thenMessageIsStored() {
        topic.publish("topic-1", "hello");

        List<InMemoryMessageTopic.PublishedMessage> messages = topic.getPublishedMessages();
        assertEquals(1, messages.size());
        assertEquals("topic-1", messages.get(0).topicId());
        assertEquals("hello", messages.get(0).message());
    }

    @Test
    void givenMessageWithAttributes_whenPublish_thenAttributesArePreserved() {
        Map<String, String> attrs = Map.of("key1", "value1", "key2", "value2");
        topic.publish("topic-1", "hello", attrs);

        List<InMemoryMessageTopic.PublishedMessage> messages = topic.getPublishedMessages();
        assertEquals(1, messages.size());
        assertEquals("value1", messages.get(0).attributes().get("key1"));
        assertEquals("value2", messages.get(0).attributes().get("key2"));
    }

    @Test
    void givenSubscriber_whenPublish_thenSubscriberIsNotified() {
        AtomicReference<String> received = new AtomicReference<>();
        AtomicReference<Map<String, String>> receivedAttrs = new AtomicReference<>();

        topic.subscribe("topic-1", (msg, attrs) -> {
            received.set(msg);
            receivedAttrs.set(attrs);
        });

        topic.publish("topic-1", "hello", Map.of("attr", "val"));

        assertEquals("hello", received.get());
        assertEquals("val", receivedAttrs.get().get("attr"));
    }

    @Test
    void givenSubscriberOnDifferentTopic_whenPublish_thenNotNotified() {
        AtomicReference<String> received = new AtomicReference<>();

        topic.subscribe("topic-other", (msg, attrs) -> received.set(msg));

        topic.publish("topic-1", "hello");

        assertNull(received.get());
    }

    @Test
    void givenMultipleTopics_whenGetPublishedMessagesByTopic_thenFiltersCorrectly() {
        topic.publish("topic-a", "msg-a");
        topic.publish("topic-b", "msg-b1");
        topic.publish("topic-b", "msg-b2");

        List<InMemoryMessageTopic.PublishedMessage> topicAMessages = topic.getPublishedMessages("topic-a");
        List<InMemoryMessageTopic.PublishedMessage> topicBMessages = topic.getPublishedMessages("topic-b");

        assertEquals(1, topicAMessages.size());
        assertEquals(2, topicBMessages.size());
    }

    @Test
    void givenPublishWithoutAttributes_whenPublish_thenEmptyAttributesStored() {
        topic.publish("topic-1", "hello");

        List<InMemoryMessageTopic.PublishedMessage> messages = topic.getPublishedMessages();
        assertTrue(messages.get(0).attributes().isEmpty());
    }

    @Test
    void givenMessages_whenClear_thenAllDataRemoved() {
        topic.publish("topic-1", "msg1");
        topic.subscribe("topic-1", (msg, attrs) -> {});

        topic.clear();

        assertTrue(topic.getPublishedMessages().isEmpty());
    }

    @Test
    void givenPublishedMessages_whenGetAll_thenListIsUnmodifiable() {
        topic.publish("topic-1", "msg1");

        List<InMemoryMessageTopic.PublishedMessage> messages = topic.getPublishedMessages();

        assertThrows(
                UnsupportedOperationException.class,
                () -> messages.add(new InMemoryMessageTopic.PublishedMessage("x", "y", Map.of())));
    }
}
