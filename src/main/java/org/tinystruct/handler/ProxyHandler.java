package org.tinystruct.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;

public interface ProxyHandler {
    void addCodec(ChannelPipeline pipeline, ChannelHandler... handler);

    ChannelHandler[] initCodecs();
}
