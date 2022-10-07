package org.tinystruct.valve;

import org.tinystruct.ApplicationException;

import java.util.concurrent.TimeUnit;

public interface Lock {
    void lock();

    boolean tryLock();

    boolean tryLock(long time, TimeUnit unit) throws ApplicationException;

    void unlock();

    String id();
}