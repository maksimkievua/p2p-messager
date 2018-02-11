package com.indev.p2pmessenger.protocol.dto;

import lombok.Value;

@Value
public class Contact {
    private String name;
    private String ip;
    private Integer port;
}
