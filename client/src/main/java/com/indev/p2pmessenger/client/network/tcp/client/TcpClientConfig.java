package com.indev.p2pmessenger.client.network.tcp.client;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class TcpClientConfig {
    private final String host;
    private final int port;
    private final int connectionTimeoutMillis;
    private final int socketTimeoutMillis;
    private final int maxMessagesToRead;

    public static TcpClientConfig defaultConfig(String host, int port) {
        return custom(host, port).build();
    }

    public static TcpClientConfigBuilder custom(String host, int port) {
        return builder()
                .host(host)
                .port(port)
                .connectionTimeoutMillis(10000)
                .socketTimeoutMillis(10000)
                .maxMessagesToRead(10);
    }
}
