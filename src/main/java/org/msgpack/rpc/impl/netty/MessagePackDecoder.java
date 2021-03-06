package org.msgpack.rpc.impl.netty;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.EOFException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MessagePackDecoder extends ByteToMessageDecoder {

    private static final boolean DEBUG = false;
    private final static Logger LOGGER = LoggerFactory.getLogger(MessagePackDecoder.class);

    private final ObjectMapper mapper;

    public MessagePackDecoder(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void decode(final ChannelHandlerContext channelHandlerContext,
                          final ByteBuf byteBuf,
                          final List<Object> out) throws Exception {
        if (!byteBuf.isReadable()) {
            return;
        }

        byteBuf.markReaderIndex();
        try {
            JsonNode node = mapper.readTree(new ByteBufInputStream(byteBuf));
            if (DEBUG) {
                // Create a JSON mapper.
                ObjectMapper jsonMapper = new ObjectMapper();
                LOGGER.info(jsonMapper.writeValueAsString(node));
            }

            if (node.isArray()) {
                out.add(node);
            }
        } catch (EOFException e) {
            byteBuf.resetReaderIndex();
        }
    }
}
