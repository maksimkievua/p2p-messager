package com.indev.p2pmessenger.client.network;

import com.indev.p2pmessenger.client.network.tcp.client.TcpClient;
import com.indev.p2pmessenger.client.network.tcp.client.TcpClientConfig;
import com.indev.p2pmessenger.protocol.dto.ChatMessage;
import com.indev.p2pmessenger.protocol.dto.network.message.chat.MessageSentEvent;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public class ChatClient implements Closeable {

    private final TcpClient tcpClient;

    public ChatClient(String host, int port) {
        TcpClientConfig tcpClientConfig = TcpClientConfig.defaultConfig(host, port);
        tcpClient = new TcpClient(tcpClientConfig, this::onEvent);
    }

    public void connect() {
        tcpClient.connect();
    }

    @Override
    public void close() {
        tcpClient.close();
    }

    public CompletableFuture<Void> sendMessage(ChatMessage chatMessage) {
        return tcpClient.sendEvent(new MessageSentEvent(chatMessage));
    }

    private void onEvent(Object event) {
        throw new UnsupportedOperationException("Should not accept incoming events");
    }
}
