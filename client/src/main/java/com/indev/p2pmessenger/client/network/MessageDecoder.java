package com.indev.p2pmessenger.client.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indev.p2pmessenger.protocol.dto.network.Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class MessageDecoder extends MessageToMessageDecoder<ByteBuf> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        out.add(mapper.readValue(ByteBufUtil.getBytes(msg), Message.class));
    }
}
