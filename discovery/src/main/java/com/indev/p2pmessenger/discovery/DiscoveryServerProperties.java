package com.indev.p2pmessenger.discovery;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("discovery.server")
@Component
@Data
public class DiscoveryServerProperties {
    private int port;
}
