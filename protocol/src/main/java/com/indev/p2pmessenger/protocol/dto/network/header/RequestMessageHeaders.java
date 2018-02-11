package com.indev.p2pmessenger.protocol.dto.network.header;

import lombok.Value;

@Value
public class RequestMessageHeaders implements MessageHeaders {
    private final Long correlationId;
}
