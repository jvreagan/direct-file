package gov.irs.directfile.models.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryMessageQueueTest {
    private InMemoryMessageQueue queue;

    @BeforeEach
    void setUp() {
        queue = new InMemoryMessageQueue();
    }

    @Test
    void givenMessage_whenSendAndReceive_thenMessageIsReturned() {
        queue.send("test-queue", "hello", Map.of("key", "value"));

        List<QueueMessageEnvelope> messages = queue.receive("test-queue", 10, 0);

        assertEquals(1, messages.size());
        assertEquals("hello", messages.get(0).body());
        assertEquals("value", messages.get(0).attributes().get("key"));
    }

    @Test
    void givenMessage_whenReceived_thenRemovedFromQueue() {
        queue.send("test-queue", "hello");

        List<QueueMessageEnvelope> first = queue.receive("test-queue", 10, 0);
        assertEquals(1, first.size());

        List<QueueMessageEnvelope> second = queue.receive("test-queue", 10, 0);
        assertTrue(second.isEmpty());
    }

    @Test
    void givenMultipleMessages_whenReceiveWithMaxMessages_thenRespectsLimit() {
        queue.send("test-queue", "msg1");
        queue.send("test-queue", "msg2");
        queue.send("test-queue", "msg3");

        List<QueueMessageEnvelope> messages = queue.receive("test-queue", 2, 0);

        assertEquals(2, messages.size());
        assertEquals(1, queue.size("test-queue"));
    }

    @Test
    void givenEmptyQueue_whenReceive_thenReturnsEmptyList() {
        List<QueueMessageEnvelope> messages = queue.receive("empty-queue", 10, 0);

        assertTrue(messages.isEmpty());
    }

    @Test
    void givenTwoQueues_whenSendToOne_thenOtherIsUnaffected() {
        queue.send("queue-a", "message-a");
        queue.send("queue-b", "message-b");

        List<QueueMessageEnvelope> fromA = queue.receive("queue-a", 10, 0);
        List<QueueMessageEnvelope> fromB = queue.receive("queue-b", 10, 0);

        assertEquals(1, fromA.size());
        assertEquals("message-a", fromA.get(0).body());
        assertEquals(1, fromB.size());
        assertEquals("message-b", fromB.get(0).body());
    }

    @Test
    void givenMessages_whenDelete_thenIsNoOp() {
        queue.send("test-queue", "hello");
        List<QueueMessageEnvelope> messages = queue.receive("test-queue", 10, 0);

        // delete is a no-op for in-memory (message already removed on receive)
        assertDoesNotThrow(() -> queue.delete("test-queue", messages.get(0).receiptHandle()));
    }

    @Test
    void givenMessages_whenClear_thenAllQueuesEmpty() {
        queue.send("queue-a", "msg1");
        queue.send("queue-b", "msg2");

        queue.clear();

        assertEquals(0, queue.size("queue-a"));
        assertEquals(0, queue.size("queue-b"));
    }

    @Test
    void givenMessages_whenSize_thenReturnsCorrectCount() {
        assertEquals(0, queue.size("test-queue"));

        queue.send("test-queue", "msg1");
        queue.send("test-queue", "msg2");

        assertEquals(2, queue.size("test-queue"));
    }

    @Test
    void givenConcurrentThreads_whenSendAndReceive_thenNoMessagesLost() throws InterruptedException {
        int threadCount = 10;
        int messagesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                for (int i = 0; i < messagesPerThread; i++) {
                    queue.send("concurrent-queue", "msg-" + i);
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        int totalExpected = threadCount * messagesPerThread;
        assertEquals(totalExpected, queue.size("concurrent-queue"));

        List<QueueMessageEnvelope> allMessages = new ArrayList<>();
        while (true) {
            List<QueueMessageEnvelope> batch = queue.receive("concurrent-queue", 100, 0);
            if (batch.isEmpty()) break;
            allMessages.addAll(batch);
        }

        assertEquals(totalExpected, allMessages.size());
    }
}
