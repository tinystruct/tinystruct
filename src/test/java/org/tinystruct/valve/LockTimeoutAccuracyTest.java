package org.tinystruct.valve;

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class LockTimeoutAccuracyTest {

    @Test
    public void testTimeoutAccuracy() throws ApplicationException, InterruptedException {
        Lock lock1 = new DistributedLock();
        Lock lock2 = new DistributedLock(lock1.id().getBytes());

        // Thread 1 acquires the lock
        lock1.lock();
        try {
            // Thread 2 tries to acquire with timeout
            long startTime = System.nanoTime();
            boolean acquired = lock2.tryLock(2, TimeUnit.SECONDS);
            long endTime = System.nanoTime();
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

            assertFalse(acquired, "Lock should not be acquired");
            System.out.println("Elapsed time: " + elapsedMs + "ms");

            // Allow some tolerance (100ms) for scheduling overhead
            assertTrue(elapsedMs >= 2000, "Should wait at least 2000ms, but waited " + elapsedMs + "ms");
            assertTrue(elapsedMs < 2200, "Should not wait more than 2200ms, but waited " + elapsedMs + "ms");
        } finally {
            // Must always release, even on assertion failure - lock1's id is a JVM-wide
            // singleton (Watcher) entry backed by a real file (.lock); leaking it here would
            // poison every later test that touches the lock subsystem in this JVM.
            lock1.unlock();
        }
    }

    @Test
    public void testSuccessfulAcquisitionTiming() throws ApplicationException, InterruptedException {
        Lock lock1 = new DistributedLock();
        Lock lock2 = new DistributedLock(lock1.id().getBytes());

        // DistributedLock.unlock() enforces that only the thread which acquired the lock
        // may release it (like ReentrantLock). So the "other holder" releasing lock1 after
        // 1 second must both acquire and release it on the same thread - acquiring it on
        // the main thread and unlocking from a separate releaser thread throws
        // IllegalMonitorStateException and never actually releases the lock.
        CountDownLatch acquiredByHolder = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            try {
                lock1.lock();
                acquiredByHolder.countDown();
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock1.unlock();
            }
        });
        holder.start();
        // Wait until the holder thread has actually acquired lock1 before starting the
        // stopwatch, otherwise tryLock() below could race ahead of it and acquire
        // immediately, measuring ~0ms instead of the intended ~1 second wait.
        acquiredByHolder.await();

        // Thread 2 tries to acquire with 5 second timeout
        boolean acquired = false;
        try {
            long startTime = System.nanoTime();
            acquired = lock2.tryLock(5, TimeUnit.SECONDS);
            long endTime = System.nanoTime();
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

            assertTrue(acquired, "Lock should be acquired");
            System.out.println("Acquisition time: " + elapsedMs + "ms");

            // Should acquire shortly after 1 second (allow up to 1.5 seconds for overhead)
            assertTrue(elapsedMs >= 1000, "Should wait at least 1000ms, but waited " + elapsedMs + "ms");
            assertTrue(elapsedMs < 1500, "Should acquire within 1500ms, but took " + elapsedMs + "ms");
        } finally {
            // Must always release (if acquired) and reap the holder thread, even on
            // assertion failure - lock1/lock2 share an id backed by a JVM-wide singleton
            // (Watcher) and a real file (.lock); leaking it here would poison every later
            // test that touches the lock subsystem in this JVM.
            if (acquired) {
                lock2.unlock();
            }
            holder.join();
        }
    }
}
