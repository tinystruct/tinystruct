package org.tinystruct.handler;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;

public class RedisProxyHandler extends ProxyInboundHandler {

    public RedisProxyHandler(String remoteHost, int remotePort) {
        super(remoteHost, remotePort);
    }

    @Override
    public ChannelHandler[] initCodecs() {
        return new ChannelHandler[]{new RedisDecoder(true), new RedisBulkStringAggregator(), new RedisArrayAggregator(), new RedisEncoder()};
    }

}
