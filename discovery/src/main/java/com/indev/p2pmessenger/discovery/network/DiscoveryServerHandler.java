package com.indev.p2pmessenger.discovery.network;

import com.indev.p2pmessenger.protocol.dto.Contact;
import com.indev.p2pmessenger.protocol.dto.network.Message;
import com.indev.p2pmessenger.protocol.dto.network.MessageType;
import com.indev.p2pmessenger.protocol.dto.network.header.RequestMessageHeaders;
import com.indev.p2pmessenger.protocol.dto.network.header.ResponseMessageHeaders;
import com.indev.p2pmessenger.protocol.dto.network.message.discovery.*;
import io.netty.channel.*;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
@Slf4j
@ChannelHandler.Sharable
public class DiscoveryServerHandler extends ChannelInboundHandlerAdapter {

    private final Map<ChannelId, Session> sessions = new ConcurrentHashMap<>();

    private static Message successResponse(Long correlationId, Object payload) {
        return Message.response(new ResponseMessageHeaders(correlationId, null), payload);
    }

    private static Message errorResponse(Long correlationId, String errorMessage) {
        return Message.response(new ResponseMessageHeaders(correlationId, errorMessage), null);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Session inactiveSession = sessions.remove(ctx.channel().id());
        if (inactiveSession != null) {
            log.info("Contact disappeared: {}", inactiveSession.getContact());
            broadcast(new ContactDisappeared(inactiveSession.getContact()));
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message message = (Message) msg;
        if (message.getType() != MessageType.REQUEST) {
            log.warn("Unexpected message type: {}", message);
            return;
        }
        log.debug("Received request from {}: {}", ctx.channel().remoteAddress(), message);
        RequestMessageHeaders headers = (RequestMessageHeaders) message.getHeaders();
        Message responseMessage = processRequestMessage(ctx, headers, message.getPayload());
        send(ctx.channel(), responseMessage);
        super.channelRead(ctx, msg);
    }

    private Message processRequestMessage(ChannelHandlerContext ctx, RequestMessageHeaders headers, Object payload) {
        try {
            Object responsePayload = processRequest(ctx, payload);
            return successResponse(headers.getCorrelationId(), responsePayload);
        } catch (Exception e) {
            log.error("Unexpected exception", e);
            return errorResponse(headers.getCorrelationId(), e.getMessage());
        }
    }

    private Object processRequest(ChannelHandlerContext ctx, Object request) {
        if (request instanceof LoginRequest) {
            LoginRequest loginRequest = (LoginRequest) request;
            handleLogin(loginRequest, ctx.channel());
            return null;
        }
        if (request instanceof GetContactsRequest) {
            List<Contact> contacts = sessions.values()
                    .stream()
                    .map(Session::getContact)
                    .collect(toList());
            return new GetContactsResponse(contacts);
        }
        throw new IllegalArgumentException("Unknown request type");
    }

    private void handleLogin(LoginRequest loginRequest, Channel channel) {
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        Contact contact = new Contact(loginRequest.getName(), remoteAddress.getHostName(),
                loginRequest.getAdvertisedPort());
        log.info("Discovered new contact: {}", contact);
        broadcast(new ContactAppeared(contact));
        sessions.put(channel.id(), new Session(channel, contact));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error(cause.getMessage(), cause);
        ctx.close();
    }

    private void send(Channel channel, Message message) {
        log.debug("Sending message to {}: {}", channel.remoteAddress(), message);
        channel.writeAndFlush(message).addListener(future -> {
            if (!future.isSuccess()) {
                log.error("Failed to send message {}", message, future.cause());
            }
        });
    }

    private void broadcast(Object event) {
        Message message = Message.event(event);
        log.debug("Broadcasting message to {} clients: {}", sessions.size(), message);
        sessions.values().forEach(session -> send(session.getChannel(), message));
    }

    @Value
    private static class Session {
        private final Channel channel;
        private final Contact contact;
    }
}
