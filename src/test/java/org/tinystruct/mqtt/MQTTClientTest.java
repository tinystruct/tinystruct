package org.tinystruct.mqtt;

import org.junit.jupiter.api.Test;
import org.tinystruct.system.Callback;
import org.tinystruct.system.Event;
import org.tinystruct.system.Settings;

class MQTTClientTest {

    MQTTClient client = new MQTTClient(new Settings());

    @Test
    void publish() {
        subscribe();
        client.publish("@test@", "Praise the Lord!");
    }

    @Test
    void subscribe() {
        client.subscribe("@test@", new MessageListener<Event>() {
            @Override
            public void onMessage(String topic, Event message) {
                if (topic.equalsIgnoreCase("@test@")) {
                    this.on(message, event -> System.out.println("OK:" + event.getPayload()));
                }
            }

            @Override
            public void on(Event event, Callback<Event> callback) {
                callback.process(event);
            }
        });
    }
}