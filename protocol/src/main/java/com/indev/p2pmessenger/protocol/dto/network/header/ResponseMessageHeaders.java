package com.indev.p2pmessenger.protocol.dto.network.header;

import lombok.Value;

@Value
public class ResponseMessageHeaders implements MessageHeaders {
    private final Long correlationId;
    private final String errorMessage;
}
