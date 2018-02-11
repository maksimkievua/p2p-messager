package com.indev.p2pmessenger.client.network.tcp.client;

import com.indev.p2pmessenger.protocol.dto.network.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TcpClientHandler extends ChannelInboundHandlerAdapter {
    private final TcpClient client;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.debug("Received message {}", msg);
        if (!(msg instanceof Message)) {
            throw new IllegalArgumentException("Wrong msg type " + msg.getClass().getName());
        }
        client.onMessage((Message) msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Connected to {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Disconnected from {}", ctx.channel().remoteAddress());
        client.onDisconnect();
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        client.onException(cause);
    }
}
