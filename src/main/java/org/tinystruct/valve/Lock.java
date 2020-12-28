package org.tinystruct.valve;

import org.tinystruct.ApplicationException;

import java.util.concurrent.TimeUnit;

public interface Lock {
    void lock() throws ApplicationException;

    boolean tryLock() throws ApplicationException;

    boolean tryLock(long time, TimeUnit unit) throws ApplicationException;

    void unlock() throws ApplicationException;

    String id();
}