package org.msgpack.rpc.impl.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.msgpack.rpc.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MessagePackEncoder extends MessageToByteEncoder<Message> {

    private static final boolean DEBUG = false;
    private final static Logger LOGGER = LoggerFactory.getLogger(MessagePackDecoder.class);

    private final ObjectMapper mapper;

    public MessagePackEncoder(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message message, ByteBuf out) throws Exception {
        ArrayNode node = message.toObjectArray(mapper);
        if (DEBUG) {
            // Create a JSON mapper.
            ObjectMapper jsonMapper = new ObjectMapper();
            LOGGER.debug(jsonMapper.writeValueAsString(node));
        }

        mapper.writeValue(new ByteBufOutputStream(out), node);
    }
}
