package org.tinystruct.valve;

import org.tinystruct.ApplicationException;
import org.tinystruct.valve.Watcher.EventListener;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DistributedLock implements Lock {

    private String id;
    private Watcher watcher;

    public DistributedLock() {
        this.id = UUID.randomUUID().toString();
    }

    public DistributedLock(byte[] id) {
        this.id = new String(id);
    }

    @Override
    public void lock() throws ApplicationException {
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
        watcher = Watcher.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);

        watcher.setListener(new EventListener() {
            @Override
            public void onUpdate() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onDelete(String lockId) {
                // TODO Auto-generated method stub
                latch.countDown();
            }

            @Override
            public void onCreate(String lockId) {
                // TODO Auto-generated method stub

            }
        });

        // If the lock is existing, then wait for it to be released.
        if (watcher.watch(this)) {
            try {
                if (timeout > 0)
                    latch.await(timeout, unit);
                else
                    latch.await();
            } catch (InterruptedException e) {
                throw new ApplicationException(e.getMessage(), e.getCause());
            }
        }

        watcher.register(this);
        return true;
    }

    @Override
    public void unlock() throws ApplicationException {
        watcher = Watcher.getInstance();
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