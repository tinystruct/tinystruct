package org.tinystruct.system;

import org.tinystruct.mqtt.MessageListener;

public interface MessageQueue<T> {
    default void subscribe(String topic, MessageListener listener) {

    }

    default void publish(String topic, String message) {

    }

    void send(T message);
    void close();
}
