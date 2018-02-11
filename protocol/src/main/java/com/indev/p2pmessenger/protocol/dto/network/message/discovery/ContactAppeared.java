package com.indev.p2pmessenger.protocol.dto.network.message.discovery;

import com.indev.p2pmessenger.protocol.dto.Contact;
import lombok.Value;

@Value
public class ContactAppeared {
    private final Contact contact;
}
