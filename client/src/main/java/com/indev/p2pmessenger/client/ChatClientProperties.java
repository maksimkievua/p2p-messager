package com.indev.p2pmessenger.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.SocketUtils;

@ConfigurationProperties("chat.client")
@Component
@Data
public class ChatClientProperties {
    private String discoveryServiceHost;
    private int discoveryServicePort;

    private int port = SocketUtils.findAvailableTcpPort();
}
