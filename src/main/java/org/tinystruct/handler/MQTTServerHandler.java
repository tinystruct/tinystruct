package org.tinystruct.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.CharsetUtil;

/**
 * MQTT Server handler
 *
 * @author James ZHOU
 */
public class MQTTServerHandler extends SimpleChannelInboundHandler<MqttMessage> {
    public static final MQTTServerHandler INSTANCE = new MQTTServerHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage message) throws Exception {
        // Handle different MQTT message types
        if (message instanceof MqttConnectMessage) {
            // Handle connection requests
            handleConnect(ctx, (MqttConnectMessage) message);
        } else if (message instanceof MqttPublishMessage) {
            // Handle incoming published messages
            handlePublish(ctx, (MqttPublishMessage) message);
        } else if (message instanceof MqttSubscribeMessage) {
            // Handle subscription requests
            handleSubscribe(ctx, (MqttSubscribeMessage) message);
        }
        // Add handling for other MQTT message types as needed
    }

    private void handleConnect(ChannelHandlerContext ctx, MqttConnectMessage connectMessage) {
        // Handle connection request
        // Extract client ID, username, password, etc. from the connect message
        // Perform authentication, if required
        // Send connection acknowledgement
        MqttConnAckMessage connAckMessage = MqttMessageBuilders.connAck()
                .returnCode(MqttConnectReturnCode.CONNECTION_ACCEPTED)
                .build();
        ctx.writeAndFlush(connAckMessage);
    }

    private void handlePublish(ChannelHandlerContext ctx, MqttPublishMessage publishMessage) {
        // Handle incoming published message
        // Extract topic, message payload, QoS, etc. from the publish message
        // Process the message and send appropriate responses
        String topic = publishMessage.variableHeader().topicName();
        ByteBuf payload = publishMessage.payload();
        // Convert payload ByteBuf to a string
        String message = payload.toString(CharsetUtil.UTF_8);

        // Process the received message based on the topic and payload
        System.out.println("Received message on topic: " + topic);
        System.out.println("Message: " + message);
        persist(topic, message);

        // Send an acknowledgement (if required)
        MqttMessage mqttAck = MqttMessageBuilders.pubAck()
                .packetId(publishMessage.variableHeader().packetId())
                .build();
        ctx.writeAndFlush(mqttAck);
    }

    private void handleSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage subscribeMessage) {
        // Handle subscription request
        // Extract topic subscriptions from the subscribe message
        // Add the client to the list of subscribers for the subscribed topics
        // Send subscription acknowledgement
        MqttSubAckMessage subAckMessage = MqttMessageBuilders.subAck()
                .addGrantedQos(MqttQoS.AT_MOST_ONCE) // QoS level 0 for all subscriptions
                .packetId(subscribeMessage.variableHeader().messageId())
                .build();
        ctx.writeAndFlush(subAckMessage);
    }

    protected void persist(String topic, String message) {
        //TODO:
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // Handle exceptions
        cause.printStackTrace();
        ctx.close();
    }
}

