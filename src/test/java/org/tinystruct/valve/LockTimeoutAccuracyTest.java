package org.tinystruct.valve;

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class LockTimeoutAccuracyTest {

    @Test
    public void testTimeoutAccuracy() throws ApplicationException, InterruptedException {
        Lock lock1 = new DistributedLock();
        Lock lock2 = new DistributedLock(lock1.id().getBytes());

        // Thread 1 acquires the lock
        lock1.lock();

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

        lock1.unlock();
    }

    @Test
    public void testSuccessfulAcquisitionTiming() throws ApplicationException, InterruptedException {
        Lock lock1 = new DistributedLock();
        Lock lock2 = new DistributedLock(lock1.id().getBytes());

        // Thread 1 acquires the lock
        lock1.lock();

        // Start a thread to release after 1 second
        Thread releaser = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
                lock1.unlock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        releaser.start();

        // Thread 2 tries to acquire with 5 second timeout
        long startTime = System.nanoTime();
        boolean acquired = lock2.tryLock(5, TimeUnit.SECONDS);
        long endTime = System.nanoTime();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        assertTrue(acquired, "Lock should be acquired");
        System.out.println("Acquisition time: " + elapsedMs + "ms");

        // Should acquire shortly after 1 second (allow up to 1.5 seconds for overhead)
        assertTrue(elapsedMs >= 1000, "Should wait at least 1000ms, but waited " + elapsedMs + "ms");
        assertTrue(elapsedMs < 1500, "Should acquire within 1500ms, but took " + elapsedMs + "ms");

        lock2.unlock();
        releaser.join();
    }
}
