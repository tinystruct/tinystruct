package org.tinystruct.data.tools;

public interface MessageQueue<T> {
    void send(T message);
    void close();
}
