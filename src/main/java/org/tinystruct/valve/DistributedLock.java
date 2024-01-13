package org.tinystruct.valve;

import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Distributed lock implementation based on the File system.
 * Usage:
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

    private static final Logger logger = Logger.getLogger(Watcher.class.getName());

    /**
     * Constructor to create a new DistributedLock instance with a random UUID as the lock ID.
     */
    public DistributedLock() {
        this.id = UUID.randomUUID().toString();
        this.watcher = Watcher.getInstance();
        // Set event listener.
        this.watcher.addListener(new Watcher.LockEventListener(this));
    }

    /**
     * Constructor to create a new DistributedLock instance with a specified lock ID.
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
     * Acquires the lock. If the lock is not available, it waits until the lock is released.
     */
    @Override
    public void lock() {
        // If try lock successfully, then the lock does exist, then don't need to lock.
        // And continue to work on the next steps.
        while (!tryLock()) {
            logger.info("Waiting for lock to be released...");
        }
    }

    /**
     * Attempts to acquire the lock without waiting. Returns true if the lock was acquired successfully.
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
     * @return True if the lock was acquired successfully within the specified time, false otherwise.
     * @throws ApplicationException If an error occurs during lock acquisition.
     */
    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws ApplicationException {
        // If the lock is existing, then wait for it to be released.
        if (watcher.watch(this)) {
            try {
                if (timeout > 0)
                    watcher.waitFor(this.id, timeout, unit);
                else
                    watcher.waitFor(this.id);
            } catch (InterruptedException e) {
                logger.severe("Error while waiting for lock: " + e.getMessage());
                throw new ApplicationException(e.getMessage(), e.getCause());
            }
        } else {
            // Register the lock.
            this.watcher.register(this);
        }

        // If you get this step, that means the lock has not been registered, and the thread can work on the next steps.
        return true;
    }

    /**
     * Releases the lock.
     */
    @Override
    public void unlock() {
        try {
            if (watcher.watch(this)) {
                watcher.unregister(this);
            }
        } catch (ApplicationException e) {
            logger.severe("Error while unlocking: " + e.getMessage());
            throw new ApplicationRuntimeException(e.getMessage(), e.getCause());
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
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
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
