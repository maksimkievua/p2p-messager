package com.indev.p2pmessenger.protocol.dto.network.message.discovery;

import lombok.Value;

@Value
public class LoginRequest {
    private final String name;
    private final int advertisedPort;
}
