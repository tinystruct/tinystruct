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
        client.publish("@test@", "Praise to the Lord!");
    }

    @Test
    void subscribe() {
        client.subscribe("@test@", new MessageListener<Event>() {
            @Override
            public void on(Event event, Callback callback) {
                callback.process(event);
            }

            @Override
            public void onMessage(final String topic, Event evt) {
                if (topic.equalsIgnoreCase("@test@")) {
                    this.on(evt, new Callback<Event>() {
                        @Override
                        public void process(Event evt) {
                            System.out.println("OK:" + evt.getPayload());
                        }
                    });
                }
            }
        });
    }
}