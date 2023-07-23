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
        client.publish("test", "hello");
        client.publish("test", "Praise to the Lord!");
    }

    @Test
    void subscribe() {
        client.subscribe("test", new MessageListener() {
            @Override
            public void on(Event event, Callback callback) {
                callback.process();
            }

            @Override
            public void onMessage(final String topic, Object message) {
                this.on(new Event() {
                }, new MessageCallback(message) {
                    @Override
                    public void process() {
                        if(topic.equalsIgnoreCase("test")) {
                            System.out.println("OK:" + message);
                        }
                    }
                });
            }
        });
    }

    class MessageCallback implements Callback {
        Object message;
        public MessageCallback(Object message) {
            this.message = message;
        }

        public void process() {
        }
    }
}