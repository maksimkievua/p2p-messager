package com.indev.p2pmessenger.client.network;

import com.indev.p2pmessenger.client.ChatClientProperties;
import com.indev.p2pmessenger.client.network.tcp.client.TcpClient;
import com.indev.p2pmessenger.client.network.tcp.client.TcpClientConfig;
import com.indev.p2pmessenger.protocol.dto.Contact;
import com.indev.p2pmessenger.protocol.dto.network.message.discovery.GetContactsRequest;
import com.indev.p2pmessenger.protocol.dto.network.message.discovery.GetContactsResponse;
import com.indev.p2pmessenger.protocol.dto.network.message.discovery.LoginRequest;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Component
public class DiscoveryClient {

    private final AtomicLong eventListenerIdCounter = new AtomicLong();
    private final Map<Long, Consumer<Object>> eventListeners = new ConcurrentHashMap<>();

    private final TcpClient tcpClient;

    public DiscoveryClient(ChatClientProperties properties) {
        TcpClientConfig tcpClientConfig = TcpClientConfig.defaultConfig(properties.getDiscoveryServiceHost(),
                properties.getDiscoveryServicePort());
        tcpClient = new TcpClient(tcpClientConfig, this::onEvent);
    }

    @PostConstruct
    public void connect() {
        tcpClient.connect();
    }

    @PreDestroy
    public void close() {
        tcpClient.close();
    }

    public CompletableFuture<Void> login(String name, int advertisedPort) {
        return tcpClient.request(new LoginRequest(name, advertisedPort), Void.class);
    }

    public CompletableFuture<List<Contact>> getContacts() {
        return tcpClient.request(new GetContactsRequest(), GetContactsResponse.class)
                .thenApply(GetContactsResponse::getContacts);
    }

    public Long addEventListener(Consumer<Object> consumer) {
        long listenerId = eventListenerIdCounter.incrementAndGet();
        eventListeners.put(listenerId, consumer);
        return listenerId;
    }

    public void removeEventListener(Long listenerId) {
        eventListeners.remove(listenerId);
    }

    private void onEvent(Object event) {
        eventListeners.values().forEach(consumer -> consumer.accept(event));
    }
}
