package org.tinystruct.valve;

import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Distributed lock depends on File system.
 * Usage:
 * <code>
 * <p>
 * Lock lock = Watcher.getInstance().acquire();
 * </p>
 * ...
 * <p>
 * try {</p>
 * lock.lock();
 * <p>
 * // TODO </p>
 * logger.info(Thread.currentThread().getName() + " is selling #" + (tickets--) + " with Lock#" + lock.id());
 * } catch (ApplicationException e) {
 * e.printStackTrace();
 * } finally {
 * lock.unlock();
 * }
 * </code>
 *
 * @author James Zhou
 */
public class DistributedLock implements Lock {
    private final String id;
    private final Watcher watcher = Watcher.getInstance();

    public DistributedLock() {
        this.id = UUID.randomUUID().toString();
        // Set event listener.
        this.watcher.addListener(new Watcher.LockEventListener(this));
    }

    public DistributedLock(byte[] idb) {
        this.id = new LockKey(idb).value();
        // Set event listener.
        this.watcher.addListener(new Watcher.LockEventListener(this));
    }

    @Override
    public void lock() {
        // If try lock successfully, then the lock does exist, then don't need to lock.
        // And continue to work on the next steps.
        if (!tryLock()) {
            lock();
        }
    }

    @Override
    public boolean tryLock() {
        try {
            return tryLock(0L, TimeUnit.SECONDS);
        } catch (ApplicationException e) {
            throw new ApplicationRuntimeException(e.getMessage(), e.getCause());
        }
    }

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
                throw new ApplicationException(e.getMessage(), e.getCause());
            }
        } else {
            // Register the lock.
            this.watcher.register(this);
        }

        // If you get this step, that means the lock has not been registered. and the thread can work on the next steps.
        return true;
    }

    @Override
    public void unlock() {
        try {
            if (watcher.watch(this)) {
                watcher.unregister(this);
            }
        } catch (ApplicationException e) {
            throw new ApplicationRuntimeException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DistributedLock other = (DistributedLock) obj;
        if (id == null) {
            return other.id == null;
        } else return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

}