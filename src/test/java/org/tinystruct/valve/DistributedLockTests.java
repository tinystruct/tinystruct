package org.tinystruct.valve;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinystruct.ApplicationException;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DistributedLockTests {
    private static final Logger log = LoggerFactory.getLogger(DistributedLockTests.class);
    private static int tickets = 100;
    private static CountDownLatch latch;

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
        for (int i = 0; i < 20; i++) {
            new Thread(new ticket(), "Window #" + i).start();
        }
    }

    @BeforeEach
    public void setUp() throws ApplicationException {
        latch = new CountDownLatch(tickets);
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
        } catch (ApplicationException e) {
            e.printStackTrace();
        } finally {
            try {
                lock.unlock();
            } catch (ApplicationException e) {
                e.printStackTrace();
            }
        }
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
                } catch (ApplicationException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        lock.unlock();
                    } catch (ApplicationException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
