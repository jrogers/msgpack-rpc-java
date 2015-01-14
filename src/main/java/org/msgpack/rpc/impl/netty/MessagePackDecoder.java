package org.msgpack.rpc.impl.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.msgpack.MessagePack;
import org.msgpack.unpacker.Unpacker;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.List;

class MessagePackDecoder extends ByteToMessageDecoder {

    private final MessagePack _msgpack;

    public MessagePackDecoder(final MessagePack msgpack){
        _msgpack = msgpack;
    }

    @Override
    protected void decode(final ChannelHandlerContext channelHandlerContext,
                          final ByteBuf byteBuf,
                          final List<Object> out) throws Exception {

        final ByteBuffer buffer = byteBuf.markReaderIndex().nioBuffer().slice();

        try{
            Unpacker unpacker = _msgpack.createBufferUnpacker(buffer);

            out.add(unpacker.readValue());

            byteBuf.skipBytes(buffer.position());
        }
        catch(EOFException e){

            byteBuf.resetReaderIndex();
        }
    }
}
