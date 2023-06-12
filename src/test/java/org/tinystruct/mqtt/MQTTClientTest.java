package org.tinystruct.data.tools;

import org.junit.jupiter.api.Test;
import org.tinystruct.mqtt.MQTTClient;
import org.tinystruct.mqtt.MessageListener;
import org.tinystruct.system.Settings;

class MQTTClientTest {

    MQTTClient client = new MQTTClient(new Settings());

    @Test
    void publish() {
        subscribe();
        client.publish("test", "hello");
        client.publish("test", "Praise to the Lord!");
    }

    @Test
    void subscribe() {
        client.subscribe("test", new MessageListener() {
            @Override
            public void onMessage(String topic, Object message) {
                System.out.println(message);
            }
        });
    }
}