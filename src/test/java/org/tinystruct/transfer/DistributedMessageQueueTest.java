package org.tinystruct.transfer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;

import java.util.ArrayDeque;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class DistributedMessageQueueTest {
    private DistributedMessageQueue queue;

    @BeforeEach
    void setUp() {
        queue = new DistributedMessageQueue();
        queue.init();
    }

    @Test
    void testPutAndTake() throws ApplicationException {
        String groupId = "testGroup";
        String sessionId = "testSession";
        String message = "Hello, World!";

        // Register session in the group
        queue.sessions.put(groupId, Set.of(sessionId));
        queue.list.put(sessionId, new ArrayDeque<>());

        // Put message in the queue
        String putResult = queue.put(groupId, sessionId, message);
        assertFalse(putResult.isEmpty());

        // Take message from the queue
        String takeResult = queue.take(sessionId);
        assertFalse(takeResult.isEmpty());
        assertTrue(takeResult.contains(message));
    }

    @Test
    void testPutWithNullMessage() {
        String groupId = "testGroup";
        String sessionId = "testSession";

        String result = queue.put(groupId, sessionId, null);
        assertEquals("{}", result);
    }

    @Test
    void testTakeFromEmptyQueue() throws ApplicationException {
        String sessionId = "testSession";
        queue.list.put(sessionId, new ArrayDeque<>());

        String result = queue.take(sessionId);
        assertEquals("{}", result);
    }

    @Test
    void testThreadSafety() throws InterruptedException, ApplicationException {
        String groupId = "testGroup";
        String sessionId = "testSession";
        queue.sessions.put(groupId, Set.of(sessionId));
        queue.list.put(sessionId, new LinkedBlockingQueue<>());

        ExecutorService executor = Executors.newFixedThreadPool(10);
        int messageCount = 100;

        CountDownLatch latch = new CountDownLatch(messageCount);
        for (int i = 0; i < messageCount; i++) {
            final int index = i;
            executor.execute(() -> {
                String put = queue.put(groupId, sessionId, "Message " + index);
                assertNotEquals(put,"{}");
                latch.countDown();
            });
        }
        latch.await();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        Thread.sleep(500);

        int receivedMessages = 0;
        while (!queue.list.get(sessionId).isEmpty()) {
            assertNotEquals("{}", queue.take(sessionId));
            receivedMessages++;
        }

        assertEquals(messageCount, receivedMessages);
    }
}
