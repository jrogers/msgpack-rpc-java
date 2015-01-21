package org.msgpack.rpc.impl.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.msgpack.rpc.message.Message;

class MessagePackEncoder extends MessageToByteEncoder<Message> {

    private final ObjectMapper mapper;

    public MessagePackEncoder(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message message, ByteBuf out) throws Exception {
        mapper.writeValue(new ByteBufOutputStream(out), message.toObjectArray(mapper));
    }
}
