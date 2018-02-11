package com.indev.p2pmessenger.client.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.indev.p2pmessenger.protocol.dto.network.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class MessageEncoder extends MessageToMessageEncoder<Message> {
    private final ObjectMapper mapper = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        byte[] bytes = mapper.writeValueAsBytes(msg);
        ByteBuf buf = ctx.alloc().buffer(bytes.length + 2);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
        out.add(buf);
    }
}
