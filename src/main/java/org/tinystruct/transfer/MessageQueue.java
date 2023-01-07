package org.tinystruct.transfer;

import org.tinystruct.ApplicationException;

public interface MessageQueue<T> {
    String put(Object groupId, String key, T message);

    T take(String key) throws ApplicationException;
}