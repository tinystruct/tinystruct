package org.tinystruct.mqtt;

import org.tinystruct.system.Listener;

public interface MessageListener<T> extends Listener<T> {
    void onMessage(String topic, T message);
}