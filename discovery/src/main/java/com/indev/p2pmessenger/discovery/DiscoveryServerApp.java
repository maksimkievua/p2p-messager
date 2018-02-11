package com.indev.p2pmessenger.discovery;

import com.indev.p2pmessenger.discovery.network.DiscoveryServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DiscoveryServerApp {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApp.class, args);
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    public DiscoveryServer discoveryServer(DiscoveryServerProperties properties) {
        return new DiscoveryServer(properties.getPort());
    }
}
