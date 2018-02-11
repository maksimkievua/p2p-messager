package com.indev.p2pmessenger.protocol.dto.network;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.indev.p2pmessenger.protocol.dto.network.header.EventMessageHeaders;
import com.indev.p2pmessenger.protocol.dto.network.header.MessageHeaders;
import com.indev.p2pmessenger.protocol.dto.network.header.RequestMessageHeaders;
import com.indev.p2pmessenger.protocol.dto.network.header.ResponseMessageHeaders;
import lombok.Value;

@Value
public class Message {
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private final MessageHeaders headers;

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private final Object payload;

    public static Message request(RequestMessageHeaders headers) {
        return new Message(headers, null);
    }

    public static Message request(RequestMessageHeaders headers, Object payload) {
        return new Message(headers, payload);
    }

    public static Message response(ResponseMessageHeaders headers) {
        return new Message(headers, null);
    }

    public static Message response(ResponseMessageHeaders headers, Object payload) {
        return new Message(headers, payload);
    }

    public static Message event(Object payload) {
        return new Message(new EventMessageHeaders(), payload);
    }

    @JsonIgnore
    public MessageType getType() {
        if (headers instanceof RequestMessageHeaders) {
            return MessageType.REQUEST;
        }
        if (headers instanceof ResponseMessageHeaders) {
            return MessageType.RESPONSE;
        }
        if (headers instanceof EventMessageHeaders) {
            return MessageType.EVENT;
        }
        throw new IllegalStateException("Can't determine message type for headers of type " + headers.getClass());
    }
}
