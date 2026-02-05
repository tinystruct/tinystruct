package org.tinystruct.valve;

import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Distributed lock implementation based on the File system.
 * Usage:
 * 
 * <pre>
 * {@code
 * Lock lock = Watcher.getInstance().acquire();
 * ...
 * try {
 *    lock.lock();
 *    // Critical section
 *    logger.info(Thread.currentThread().getName() + " is performing a task with Lock#" + lock.id());
 * } finally {
 *    lock.unlock();
 * }
 * }
 * </pre>
 *
 * @author James Zhou
 */
public class DistributedLock implements Lock {
    private final String id;
    private final Watcher watcher;
    private volatile Thread owner;
    private volatile int holdCount = 0;

    private static final Logger logger = Logger.getLogger(DistributedLock.class.getName());

    /**
     * Constructor to create a new DistributedLock instance with a random UUID as
     * the lock ID.
     */
    public DistributedLock() {
        this.id = UUID.randomUUID().toString();
        this.watcher = Watcher.getInstance();
        // Set event listener.
        this.watcher.addListener(new Watcher.LockEventListener(this));
    }

    /**
     * Constructor to create a new DistributedLock instance with a specified lock
     * ID.
     *
     * @param idb Byte array representing the lock ID.
     */
    public DistributedLock(byte[] idb) {
        this.id = new LockKey(idb).value();
        this.watcher = Watcher.getInstance();
        // Set event listener.
        this.watcher.addListener(new Watcher.LockEventListener(this));
    }

    public DistributedLock(String lockId) {
        this(lockId.getBytes());
    }

    /**
     * Acquires the lock. If the lock is not available, it waits until the lock is
     * released.
     */
    @Override
    public void lock() {
        Thread current = Thread.currentThread();
        // Check for reentrant lock
        if (current == owner) {
            holdCount++;
            return;
        }

        try {
            while (!tryLock(1, TimeUnit.SECONDS)) {
                logger.log(Level.FINE, "Waiting for lock to be released...");
            }
        } catch (ApplicationException e) {
            throw new ApplicationRuntimeException("Failed to acquire lock", e);
        }
    }

    /**
     * Attempts to acquire the lock without waiting. Returns true if the lock was
     * acquired successfully.
     */
    @Override
    public boolean tryLock() {
        try {
            return tryLock(0L, TimeUnit.SECONDS);
        } catch (ApplicationException e) {
            logger.severe("Error while attempting to lock: " + e.getMessage());
            throw new ApplicationRuntimeException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Attempts to acquire the lock within the specified waiting time.
     *
     * @param timeout The maximum time to wait for the lock.
     * @param unit    The time unit of the timeout parameter.
     * @return True if the lock was acquired successfully within the specified time,
     *         false otherwise.
     * @throws ApplicationException If an error occurs during lock acquisition.
     */
    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws ApplicationException {
        Thread current = Thread.currentThread();
        long deadline = timeout > 0 ? System.nanoTime() + unit.toNanos(timeout) : 0;

        while (true) {
            // Check deadline before attempting to acquire
            if (timeout > 0) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return false;
                }
            }

            synchronized (this.id.intern()) {
                // Check for reentrant lock
                if (current == owner) {
                    holdCount++;
                    return true;
                }

                // If the lock is not currently held by anyone, acquire it
                if (!watcher.watch(this)) {
                    this.watcher.register(this);
                    this.owner = current;
                    this.holdCount = 1;
                    return true;
                }
            }

            // Lock is held by another thread - wait outside the synchronized block
            if (timeout <= 0) {
                return false;
            }

            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return false;
            }

            try {
                if (!watcher.waitFor(this.id, remaining, TimeUnit.NANOSECONDS)) {
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.severe("Error while waiting for lock: " + e.getMessage());
                throw new ApplicationException(e.getMessage(), e.getCause());
            }
            // Loop back to try acquiring the lock again
        }
    }

    /**
     * Releases the lock.
     */
    @Override
    public void unlock() {
        Thread current = Thread.currentThread();
        synchronized (this.id.intern()) {
            if (owner == null)
                return;
            if (current != owner) {
                throw new IllegalMonitorStateException(
                        "Thread " + current.getName() +
                                " attempting to unlock while not holding the lock");
            }

            if (--holdCount == 0) {
                try {
                    watcher.unregister(this);
                    owner = null;
                } catch (ApplicationException e) {
                    logger.severe("Error while unlocking: " + e);
                    throw new ApplicationRuntimeException(e.getMessage(), e.getCause());
                }
            }
        }
    }

    /**
     * Gets the ID of the lock.
     *
     * @return The lock ID.
     */
    @Override
    public String id() {
        return this.id;
    }

    /**
     * Checks if two DistributedLock objects are equal.
     *
     * @param obj The object to compare with.
     * @return True if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        DistributedLock other = (DistributedLock) obj;
        return Objects.equals(id, other.id);
    }

    /**
     * Calculates the hash code of the DistributedLock object.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
