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
        logger.info("Complete all test methods.");
    }

    @Test
    public void testConcurrentTicketSelling() throws InterruptedException {
        Thread[] windows = new Thread[200];
        for (int i = 0; i < 200; i++) {
            windows[i] = new Thread(new ticket(), "Window #" + i);
            windows[i].start();
        }
        // Must join before returning: a later test in this same JVM fork (e.g.
        // DistributedLockTests.testLockTimeout, or any test elsewhere that calls
        // Watcher.getInstance().acquire()) can otherwise pick up one of these still-running
        // windows' DistributedLock while it's mid-acquisition, corrupting its ownership state.
        for (Thread window : windows) {
            window.join();
        }

        // Verified here, not in an @AfterAll: JUnit 5 doesn't guarantee this method runs
        // last, and every other test's @BeforeEach replaces the static `latch` with a fresh
        // CountDownLatch(100) that nothing but these windows ever counts down. An @AfterAll
        // awaiting whatever `latch` happens to be assigned once all methods finish hangs
        // forever if a later @BeforeEach ran after this method and replaced it.
        latch.await();
        assertEquals(0, tickets, "All tickets should be sold");
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
        // A dedicated lock, not Watcher.getInstance().acquire() - that returns *any*
        // currently-registered lock in the whole JVM, which could belong to an unrelated,
        // still-running test (e.g. testConcurrentTicketSelling's windows) and corrupt its
        // ownership state instead of giving this test an isolated lock to exercise.
        final Lock lock = new DistributedLock();

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
