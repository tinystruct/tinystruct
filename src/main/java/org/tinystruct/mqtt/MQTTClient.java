package org.tinystruct.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.tinystruct.system.Configuration;

import java.util.UUID;
import java.util.logging.Logger;

public class MQTTClient implements MessageQueue<String> {
    private final static Logger logger = Logger.getLogger(MQTTClient.class.getName());

    /**
     * Configuration
     */
    protected Configuration<String> config;
    private IMqttClient publisher;

    public MQTTClient(Configuration<String> config) {
        this.config = config;
        try {
            this.publisher = new MqttClient("tcp://" + this.config.get("mqtt.server.host") + ":" + this.config.get("mqtt.server.port"), UUID.randomUUID().toString());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            this.publisher.connect(options);
        } catch (MqttException e) {
            logger.severe(e.getMessage());
        }
    }

    @Override
    public void publish(String topic, String message) {
        if (!this.publisher.isConnected()) {
            return;
        }

        MqttMessage msg = new MqttMessage(message.getBytes());
        msg.setQos(0);
        msg.setRetained(false);
        try {
            publisher.publish(topic, msg);
        } catch (MqttException e) {
            logger.severe(e.getMessage());
        }
    }

    @Override
    public void subscribe(String topic, MessageListener listener) {
        if (!this.publisher.isConnected()) {
            return;
        }

        try {
            publisher.subscribe(topic, new IMqttMessageListener() {

                /**
                 * This method is called when a message arrives from the server.
                 *
                 * <p>
                 * This method is invoked synchronously by the MQTT client. An
                 * acknowledgment is not sent back to the server until this
                 * method returns cleanly.</p>
                 * <p>
                 * If an implementation of this method throws an <code>Exception</code>, then the
                 * client will be shut down.  When the client is next re-connected, any QoS
                 * 1 or 2 messages will be redelivered by the server.</p>
                 * <p>
                 * Any additional messages which arrive while an
                 * implementation of this method is running, will build up in memory, and
                 * will then back up on the network.</p>
                 * <p>
                 * If an application needs to persist data, then it
                 * should ensure the data is persisted prior to returning from this method, as
                 * after returning from this method, the message is considered to have been
                 * delivered, and will not be reproducible.</p>
                 * <p>
                 * It is possible to send a new message within an implementation of this callback
                 * (for example, a response to this message), but the implementation must not
                 * disconnect the client, as it will be impossible to send an acknowledgment for
                 * the message being processed, and a deadlock will occur.</p>
                 *
                 * @param topic   name of the topic on the message was published to
                 * @param message the actual message.
                 */
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    listener.onMessage(topic, new String(message.getPayload()));
                }
            });
        } catch (MqttException e) {
            logger.severe(e.getMessage());
        }
    }

    public void close() {
        try {
            publisher.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
