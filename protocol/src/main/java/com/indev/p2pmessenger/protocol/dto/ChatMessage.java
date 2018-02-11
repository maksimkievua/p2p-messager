package com.indev.p2pmessenger.protocol.dto;

import lombok.Value;

@Value
public class ChatMessage {
    private final Long timestamp;
    private final String from;
    private final String to;
    private final String message;
}
