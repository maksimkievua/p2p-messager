package com.indev.p2pmessenger.client.network;

import com.indev.p2pmessenger.client.ChatClientProperties;
import com.indev.p2pmessenger.client.network.tcp.client.FutureNotifier;
import com.indev.p2pmessenger.client.service.ChatService;
import com.indev.p2pmessenger.protocol.dto.network.Message;
import com.indev.p2pmessenger.protocol.dto.network.MessageType;
import com.indev.p2pmessenger.protocol.dto.network.message.chat.MessageSentEvent;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@ChannelHandler.Sharable
public class MessageServer extends ChannelInboundHandlerAdapter {

    private final ChatService chatService;
    private final ServerBootstrap bootstrap;

    public MessageServer(ChatClientProperties properties, ChatService chatService) {
        this.chatService = chatService;
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                new MessageEncoder(),
                                new LengthFieldBasedFrameDecoder(65536, 0, 2, 0, 2),
                                new MessageDecoder(),
                                new LoggingHandler(),
                                MessageServer.this
                        );
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .localAddress(properties.getPort())
                .validate();
    }

    @PostConstruct
    public void start() {
        ChannelFuture channelFuture = bootstrap.bind().syncUninterruptibly();
        log.info("Listening on {}", channelFuture.channel().localAddress());
    }

    @PreDestroy
    public void close() {
        CompletableFuture.allOf(
                waitForCompletion(bootstrap.config().group().shutdownGracefully()),
                waitForCompletion(bootstrap.config().childGroup().shutdownGracefully())
        ).join();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message message = (Message) msg;
        log.debug("Received message from {}: {}", ctx.channel().remoteAddress(), message);
        if (message.getType() != MessageType.EVENT) {
            log.warn("Unexpected message type: {}", message);
            return;
        }
        if (!(message.getPayload() instanceof MessageSentEvent)) {
            log.warn("Unexpected event: {}", message);
            return;
        }
        MessageSentEvent messageSentEvent = (MessageSentEvent) message.getPayload();
        chatService.onMessage(messageSentEvent.getChatMessage());
        super.channelRead(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error(cause.getMessage(), cause);
        ctx.close();
    }

    private CompletableFuture<Void> waitForCompletion(Future<?> future) {
        CompletableFuture<Object> promise = new CompletableFuture<>();
        future.addListener(new FutureNotifier<>(promise));
        return promise.thenApply(o -> null);
    }
}
