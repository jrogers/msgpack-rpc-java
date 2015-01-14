package org.msgpack.rpc.impl.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;
import org.msgpack.MessagePackable;

class MessagePackableEncoder extends MessageToByteEncoder<MessagePackable> {

    private final MessagePack _msgpack;

    public MessagePackableEncoder(final MessagePack msgpack){

        _msgpack = msgpack;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, MessagePackable msg, ByteBuf out) throws Exception {

        msg.writeTo(_msgpack.createPacker(new ByteBufOutputStream(out)));
    }
}
