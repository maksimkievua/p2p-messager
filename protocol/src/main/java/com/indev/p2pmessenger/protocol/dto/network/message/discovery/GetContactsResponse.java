package com.indev.p2pmessenger.protocol.dto.network.message.discovery;

import com.indev.p2pmessenger.protocol.dto.Contact;
import lombok.Value;

import java.util.List;

@Value
public class GetContactsResponse {
    private final List<Contact> contacts;
}
