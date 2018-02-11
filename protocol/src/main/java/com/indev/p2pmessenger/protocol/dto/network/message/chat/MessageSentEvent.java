package com.indev.p2pmessenger.protocol.dto.network.message.chat;

import com.indev.p2pmessenger.protocol.dto.ChatMessage;
import lombok.Value;

@Value
public class MessageSentEvent {
    private final ChatMessage chatMessage;
}
