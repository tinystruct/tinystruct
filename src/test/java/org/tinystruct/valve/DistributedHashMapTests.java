package org.tinystruct.valve;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Test class for DistributedHashMap
 */
public class DistributedHashMapTests {
    private static final Logger logger = Logger.getLogger(DistributedHashMapTests.class.getName());
    private DistributedHashMap<String> map;
    private static final String TEST_KEY = "testKey";
    private static final String TEST_VALUE = "testValue";

    @BeforeEach
    void setUp() throws IOException {
        map = new DistributedHashMap<>();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up any test files
        File[] files = new File(".").listFiles((dir, name) -> name.endsWith(".data"));
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    @Test
    void testBasicOperations() {
        Queue<String> queue = new LinkedList<>();
        queue.add(TEST_VALUE);

        // Test put
        assertNull(map.put(TEST_KEY, queue));
        
        // Test get
        Queue<String> retrieved = map.get(TEST_KEY);
        assertNotNull(retrieved);
        assertEquals(TEST_VALUE, retrieved.peek());

        // Test remove
        retrieved = map.remove(TEST_KEY);
        assertNotNull(retrieved);
        assertEquals(TEST_VALUE, retrieved.peek());
        assertNull(map.get(TEST_KEY));
    }

    @Test
    void testPersistence() throws IOException {
        // Put some data
        Queue<String> queue = new LinkedList<>();
        queue.add(TEST_VALUE);
        map.put(TEST_KEY, queue);

        // Create a new instance to test persistence
        DistributedHashMap<String> newMap = new DistributedHashMap<>();
        
        // Verify data was loaded
        Queue<String> retrieved = newMap.get(TEST_KEY);
        assertNotNull(retrieved);
        assertEquals(TEST_VALUE, retrieved.peek());
    }

    @Test
    void testClear() {
        // Add some data
        Queue<String> queue = new LinkedList<>();
        queue.add(TEST_VALUE);
        map.put(TEST_KEY, queue);
        
        // Clear the map
        map.clear();
        
        // Verify it's empty
        assertTrue(map.isEmpty());
        assertNull(map.get(TEST_KEY));
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "key-" + threadId + "-" + j;
                        Queue<String> queue = new LinkedList<>();
                        queue.add("value-" + threadId + "-" + j);
                        
                        // Perform operations
                        map.put(key, queue);
                        assertNotNull(map.get(key));
                        map.remove(key);
                        assertNull(map.get(key));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(0, map.size());
    }

    @Test
    void testLargeValues() {
        int queueSize = 1000;
        Queue<String> largeQueue = new LinkedList<>();
        for (int i = 0; i < queueSize; i++) {
            largeQueue.add("Value-" + i);
        }

        // Put large queue
        map.put(TEST_KEY, largeQueue);

        // Verify all values
        Queue<String> retrieved = map.get(TEST_KEY);
        assertNotNull(retrieved);
        assertEquals(queueSize, retrieved.size());
        for (int i = 0; i < queueSize; i++) {
            assertEquals("Value-" + i, retrieved.poll());
        }
    }

    @Test
    void testMultipleInstances() throws IOException {
        // Create two instances
        DistributedHashMap<String> map1 = new DistributedHashMap<>();
        DistributedHashMap<String> map2 = new DistributedHashMap<>();

        // Test concurrent access from different instances
        Queue<String> queue1 = new LinkedList<>();
        queue1.add("value1");
        map1.put("key1", queue1);

        Queue<String> queue2 = new LinkedList<>();
        queue2.add("value2");
        map2.put("key2", queue2);

        // Verify both instances can see all data
        assertNotNull(map1.get("key2"));
        assertNotNull(map2.get("key1"));
        assertEquals("value1", map2.get("key1").peek());
        assertEquals("value2", map1.get("key2").peek());
    }

    @Test
    void testErrorConditions() {
        // Test null key
        assertThrows(NullPointerException.class, () -> map.put(null, new LinkedList<>()));

        // Test null value
        assertThrows(NullPointerException.class, () -> map.put(TEST_KEY, null));

        // Test removing non-existent key
        assertNull(map.remove("nonexistent"));
    }
} 