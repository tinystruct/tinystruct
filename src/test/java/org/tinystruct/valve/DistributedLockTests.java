package org.tinystruct.valve;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinystruct.ApplicationException;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DistributedLockTests {
    private static final Logger log = LoggerFactory.getLogger(DistributedLockTests.class);
    private static int tickets = 100;
    private static CountDownLatch latch;
    private static long n;

    @AfterAll
    static void done() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(0, tickets);
        log.info("Complete all test methods.");
    }

    @Test
    public void test() throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            new Thread(new ticket(), "Window #" + i).start();
        }
    }

    @BeforeEach
    public void setUp() throws ApplicationException {
        latch = new CountDownLatch(tickets);
        n = 0L;
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testLock() {
        DistributedLock lock = new DistributedLock();
        try {
            lock.lock();
            System.out.println("Printed after locked.");
        } finally {
            lock.unlock();
        }
    }

    @Test
    void testLock2() throws InterruptedException {
//        final Lock lock = new DistributedLock("7439e9a6-0828-422f-8c86-3f9b4f7e1460".getBytes(StandardCharsets.UTF_8));
//        final ReentrantLock lock = new ReentrantLock();
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
        assertEquals(10000, n);
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
                        // TODO
                        log.info(Thread.currentThread().getName() + " is selling #" + (tickets--) + " with Lock#" + lock.id());
                    latch.countDown();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    @Test
    public void test3() throws InterruptedException, ApplicationException {
        // Create a lock
        Lock lock = Watcher.getInstance().acquire();

        // Start a thread to acquire and release the lock
        Thread thread = new Thread(() -> {
            try {
                lock.lock();
                System.out.println("Thread " + Thread.currentThread().getId() + " acquired the lock.");
                // Simulate some work
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
                System.out.println("Thread " + Thread.currentThread().getId() + " released the lock.");
            }
        });

        // Start the thread
        thread.start();

        // Wait for a while
        TimeUnit.SECONDS.sleep(1);

        // Try to acquire the lock in the main thread
        System.out.println("Main thread trying to acquire the lock.");
        if (lock.tryLock(5, TimeUnit.SECONDS)) {
            System.out.println("Main thread acquired the lock.");
            // Simulate some work
            TimeUnit.SECONDS.sleep(3);
            lock.unlock();
            System.out.println("Main thread released the lock.");
        } else {
            System.out.println("Main thread failed to acquire the lock within the timeout.");
        }

        // Wait for the thread to finish
        thread.join();
    }
}
