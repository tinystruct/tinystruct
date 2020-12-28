package org.tinystruct.transfer;

import org.tinystruct.ApplicationException;

public interface MessageQueue<T> {
    public String put(Object groupId, String key, T message);

    public T take(String key) throws ApplicationException;
}