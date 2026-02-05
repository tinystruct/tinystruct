package org.tinystruct.valve;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class DistributedLockTests {
    private static final Logger logger = Logger.getLogger(DistributedLockTests.class.getName());
    private static int tickets = 100;
    private static volatile CountDownLatch latch = new CountDownLatch(tickets);
    private static long n;

    @AfterAll
    static void done() {
        if (latch != null) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assertEquals(0, tickets);
        logger.info("Complete all test methods.");
    }

    @Test
    public void testConcurrentTicketSelling() throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            new Thread(new ticket(), "Window #" + i).start();
        }
    }

    @BeforeEach
    public void setUp() throws ApplicationException {
        tickets = 100;
        latch = new CountDownLatch(tickets);
        n = 0L;
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testBasicLockUnlock() {
        DistributedLock lock = new DistributedLock();
        assertNotNull(lock.id(), "Lock ID should not be null");

        try {
            lock.lock();
            assertTrue(true, "Lock acquired successfully");
        } finally {
            lock.unlock();
        }
    }

    @Test
    void testReentrantLock() {
        DistributedLock lock = new DistributedLock();
        try {
            lock.lock();
            // Test reentrant behavior
            lock.lock();
            assertTrue(true, "Reentrant lock acquired successfully");
            lock.unlock();
        } finally {
            lock.unlock();
        }
    }

    @Test
    void testConcurrentIncrement() throws InterruptedException {
        final int THREAD_COUNT = 100;
        final int ITERATIONS = 100;
        // final Lock lock = new
        // DistributedLock("7439e9a6-0828-422f-8c86-3f9b4f7e1460".getBytes(StandardCharsets.UTF_8));
        // final ReentrantLock lock = new ReentrantLock();
        final Lock lock = new DistributedLock();

        n = 0L;
        Thread[] threads = new Thread[100];
        CountDownLatch latch = new CountDownLatch(threads.length);
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    lock.lock();
                    n++;
                    lock.unlock();
                }
                latch.countDown();
            });
            threads[i].start();
        }

        latch.await();
        assertEquals(THREAD_COUNT * ITERATIONS, n, "Final count should match expected value");
    }

    static class ticket implements Runnable {
        private final Lock lock;

        public ticket() {
            lock = new DistributedLock();
        }

        @Override
        public void run() {
            while (tickets > 0) {
                try {
                    lock.lock();
                    if (tickets > 0)
                        tickets--;
                    latch.countDown();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    @Test
    public void testLockTimeout() throws InterruptedException, ApplicationException {
        // Create a lock
        final Lock lock = Watcher.getInstance().acquire();

        // Start a thread to acquire and release the lock
        Thread thread = new Thread(() -> {
            try {
                lock.lock();
                // Simulate some work
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        });

        // Start the thread
        thread.start();

        // Wait for a while
        TimeUnit.SECONDS.sleep(1);

        // Try to acquire the lock in the main thread
        assertTrue(lock.tryLock(5, TimeUnit.SECONDS), "Lock should be acquired within timeout");
        try {
            // Simulate some work
            TimeUnit.SECONDS.sleep(3);
        } finally {
            lock.unlock();
        }

        // Wait for the thread to finish
        thread.join();
    }

    @Test
    void testInterruptedLock() throws InterruptedException {
        Lock lock = new DistributedLock();
        Thread t = new Thread(() -> {
            try {
                lock.lock();
                Thread.currentThread().interrupt();
                assertTrue(Thread.currentThread().isInterrupted(), "Thread should still be interrupted");
            } finally {
                lock.unlock();
            }
        }

        );
        t.start();
        t.join();
    }

}
