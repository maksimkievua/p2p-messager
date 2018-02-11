package com.indev.p2pmessenger.client.network.tcp.client;

import com.indev.p2pmessenger.client.network.MessageDecoder;
import com.indev.p2pmessenger.client.network.MessageEncoder;
import com.indev.p2pmessenger.protocol.dto.network.Message;
import com.indev.p2pmessenger.protocol.dto.network.header.RequestMessageHeaders;
import com.indev.p2pmessenger.protocol.dto.network.header.ResponseMessageHeaders;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.indev.p2pmessenger.protocol.dto.network.MessageType.EVENT;
import static com.indev.p2pmessenger.protocol.dto.network.MessageType.RESPONSE;

@Slf4j
public class TcpClient {

    private final AtomicLong correlationIdCounter = new AtomicLong();
    private final Map<Long, CompletableFuture<Message>> responseMap = new ConcurrentHashMap<>();
    private final Bootstrap bootstrap;
    private final Consumer<Object> eventHandler;
    private final AtomicBoolean connected = new AtomicBoolean();
    private volatile ChannelFuture channelFuture;

    public TcpClient(TcpClientConfig config, Consumer<Object> eventHandler) {
        bootstrap = new Bootstrap()
                .remoteAddress(config.getHost(), config.getPort())
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectionTimeoutMillis())
                .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(config.getMaxMessagesToRead()))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                new LengthFieldBasedFrameDecoder(65536, 0, 2, 0, 2),
                                new LoggingHandler(),
                                new MessageDecoder(),
                                new MessageEncoder(),
                                new TcpClientHandler(TcpClient.this)
                        );
                    }
                })
                .validate();
        this.eventHandler = eventHandler;
    }

    public boolean isConnectionAlive() {
        return channelFuture.channel().isActive();
    }

    private ChannelFuture send(Message message) {
        checkConnection();
        log.debug("Sending message to {}: {}", channelFuture.channel().remoteAddress(), message);
        return channelFuture.addListener(future1 -> {
            channelFuture.channel().writeAndFlush(message).addListener(future -> {
                if (!future.isSuccess()) {
                    log.error("Failed to send message {}", message, future.cause());
                }
            });
        });
    }

    public CompletableFuture<Void> sendEvent(Object payload) {
        checkConnection();
        return waitForCompletion(send(Message.event(payload)));
    }

    public <T> CompletableFuture<T> request(Object payload, Class<T> responseType) {
        checkConnection();
        CompletableFuture<Message> responseFuture = new CompletableFuture<>();
        long correlationId = correlationIdCounter.incrementAndGet();
        responseMap.put(correlationId, responseFuture);
        RequestMessageHeaders headers = new RequestMessageHeaders(correlationId);
        send(Message.request(headers, payload));
        return responseFuture.thenApplyAsync(message -> responseType.cast(message.getPayload()));
    }

    public CompletableFuture<Void> close() {
        if (!connected.compareAndSet(true, false)) {
            return CompletableFuture.completedFuture(null);
        }

        return waitForCompletion(bootstrap.config().group().shutdownGracefully())
                .whenComplete((o, throwable) -> responseMap.clear());
    }

    private CompletableFuture<Void> waitForCompletion(Future<?> future) {
        CompletableFuture<Object> promise = new CompletableFuture<>();
        future.addListener(new FutureNotifier<>(promise));
        return promise.thenApply(o -> null);
    }

    public void connect() {
        if (connected.compareAndSet(false, true)) {
            tryConnect();
        }
    }

    private void tryConnect() {
        channelFuture = bootstrap.connect().addListener(future -> {
            if (!future.isSuccess()) {
                onDisconnect();
            }
        });
    }

    private void checkConnection() {
        if (!connected.get()) {
            throw new IllegalStateException("Connection is not alive");
        }
    }

    void onMessage(Message message) {
        if (message.getType() == RESPONSE) {
            ResponseMessageHeaders headers = (ResponseMessageHeaders) message.getHeaders();

            CompletableFuture<Message> responseFuture = responseMap.remove(headers.getCorrelationId());
            if (responseFuture != null) {
                responseFuture.complete(message);
            } else {
                log.warn("Dead message {}", message);
            }
        } else if (message.getType() == EVENT) {
            eventHandler.accept(message.getPayload());
        } else {
            log.warn("Wrong message {}", message);
        }
    }

    void onDisconnect() {
        if (connected.get()) {
            log.info("Reconnecting...");
            channelFuture.channel().eventLoop().schedule(this::tryConnect, 1000, TimeUnit.MILLISECONDS);
        }
    }

    void onException(Throwable cause) {
        log.error("Unexpected exception from channel", cause);
    }

}
