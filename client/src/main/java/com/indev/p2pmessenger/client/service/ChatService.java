package com.indev.p2pmessenger.client.service;

import com.indev.p2pmessenger.client.ChatClientProperties;
import com.indev.p2pmessenger.client.network.ChatClient;
import com.indev.p2pmessenger.client.network.DiscoveryClient;
import com.indev.p2pmessenger.protocol.dto.ChatMessage;
import com.indev.p2pmessenger.protocol.dto.Contact;
import com.indev.p2pmessenger.protocol.dto.network.message.discovery.ContactDisappeared;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final AtomicLong messageListenerIdCounter = new AtomicLong();
    private final Map<Long, Consumer<ChatMessage>> messageListeners = new ConcurrentHashMap<>();
    private final Map<String, ChatClient> chatClients = new ConcurrentHashMap<>();

    private final DiscoveryClient discoveryClient;
    private final ChatClientProperties chatClientProperties;

    @Getter
    private volatile String login;

    @PostConstruct
    public void init() {
        discoveryClient.addEventListener(event -> {
            if (event instanceof ContactDisappeared) {
                ContactDisappeared contactDisappeared = (ContactDisappeared) event;
                closeChatClient(contactDisappeared.getContact());
            }
        });
    }

    public CompletableFuture<Void> login(String name) {
        login = name;
        return discoveryClient.login(name, chatClientProperties.getPort());
    }

    public CompletableFuture<List<Contact>> loadContacts() {
        return discoveryClient.getContacts();
    }

    public Long addDiscoveryEventListener(Consumer<Object> consumer) {
        return discoveryClient.addEventListener(consumer);
    }

    public void removeDiscoveryEventListener(Long listenerId) {
        discoveryClient.removeEventListener(listenerId);
    }

    public CompletableFuture<Void> sendMessage(Contact contact, String text) {
        ChatMessage chatMessage = new ChatMessage(System.currentTimeMillis(), login, contact.getName(), text);
        ChatClient chatClient = getChatClient(contact);
        messageListeners.values().forEach(consumer -> consumer.accept(chatMessage));
        return chatClient.sendMessage(chatMessage);
    }

    public void onMessage(ChatMessage chatMessage) {
        log.debug("Message received: {}", chatMessage);
        messageListeners.values().forEach(consumer -> consumer.accept(chatMessage));
    }

    public Long addMessageListener(Consumer<ChatMessage> consumer) {
        long listenerId = messageListenerIdCounter.incrementAndGet();
        messageListeners.put(listenerId, consumer);
        return listenerId;
    }

    public void removeMessageListener(Long listenerId) {
        messageListeners.remove(listenerId);
    }

    private ChatClient getChatClient(Contact contact) {
        return chatClients.computeIfAbsent(contact.getName(), s -> {
            ChatClient client = new ChatClient(contact.getIp(), contact.getPort());
            client.connect();
            return client;
        });
    }

    private void closeChatClient(Contact contact) {
        ChatClient client = chatClients.remove(contact.getName());
        if (client != null) {
            client.close();
        }
    }
}
