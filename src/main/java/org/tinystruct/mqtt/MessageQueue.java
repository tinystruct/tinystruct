package org.tinystruct.mqtt;

public interface MessageQueue<T> {
    void subscribe(String topic, MessageListener<T> listener);

    void publish(String topic, String message);
}
