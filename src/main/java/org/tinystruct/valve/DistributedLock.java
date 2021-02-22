package org.tinystruct.valve;

import org.tinystruct.ApplicationException;
import org.tinystruct.valve.Watcher.EventListener;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Distributed lock depends on File system.
 * Usage:
 * <code>
 * Lock lock = Watcher.getInstance().acquire();
 * ...
 * try {
 *  if (lock != null) {
 *  lock.lock();
 *
 * // TODO
 *  logger.info(Thread.currentThread().getName() + " is selling #" + (tickets--) + " with Lock#" + lock.id());
 *  }
 * } catch (ApplicationException e) {
 * e.printStackTrace();
 * } finally {
 *  if (lock != null) {
 *      try {
 *        lock.unlock();
 *      } catch (ApplicationException e) {
 *          e.printStackTrace();
 *      }
 *  }
 * }
 * </code>
 *
 * @author James Zhou
 */
public class DistributedLock implements Lock {
    private static final Logger logger = Logger.getLogger(Watcher.class.getName());

    private String id;
    private final Watcher watcher = Watcher.getInstance();

    public DistributedLock() {
        this.id = UUID.randomUUID().toString();
        // Set event listener.
        this.watcher.addListener(new LockEventListener(this));
    }

    public DistributedLock(byte[] idb) {
        this.id = new LockKey(idb).value();
        // Set event listener.
        this.watcher.addListener(new LockEventListener(this));
    }

    @Override
    public void lock() throws ApplicationException {
        // If try lock successfully, then the lock does exist, then don't need to lock.
        // And continue to work on the next steps.
        if (!tryLock()) {
            lock();
        }
    }

    @Override
    public boolean tryLock() throws ApplicationException {
        return tryLock(0L, null);
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

        // If get this step, that means the lock has not been registered. and the thread can work on the next steps.
        return true;
    }

    @Override
    public void unlock() throws ApplicationException {
        if (watcher.watch(this)) {
            watcher.unregister(this);
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
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

}

/**
 * EventListener implementation for Lock.
 *
 * @author James Zhou
 */
class LockEventListener implements EventListener {
    private static final Logger logger = Logger.getLogger(Watcher.class.getName());
    private final Lock lock;
    private CountDownLatch latch;

    public LockEventListener(Lock lock) {
        this.lock = lock;
        this.latch = new CountDownLatch(1);
    }

    @Override
    public void onCreate(String lockId) {
        if (lockId.equalsIgnoreCase(lock.id())) {
            logger.info("Created " + lockId);
        }
    }

    @Override
    public void onUpdate() {

    }

    @Override
    public void onDelete(String lockId) {
        if (lockId.equalsIgnoreCase(lock.id())) {
            logger.info("Deleted " + lockId);
            latch.countDown();
        }
    }

    @Override
    public String id() {
        return lock.id();
    }

    @Override
    public void waitFor() throws InterruptedException {
        latch.await();
    }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }
}