package com.indev.p2pmessenger.discovery.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiscoveryServer {
    private final ServerBootstrap bootstrap;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    public DiscoveryServer(int port) {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        DiscoveryServerHandler handler = new DiscoveryServerHandler();

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
                                handler
                        );
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .localAddress(port)
                .validate();
    }

    public void start() {
        channelFuture = bootstrap.bind().syncUninterruptibly();
        log.info("Listening on {}", channelFuture.channel().localAddress());
    }

    public void close() {
        channelFuture.channel().closeFuture().syncUninterruptibly();
        bossGroup.shutdownGracefully().syncUninterruptibly();
        workerGroup.shutdownGracefully().syncUninterruptibly();
    }
}
