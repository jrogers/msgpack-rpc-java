package org.msgpack.rpc.loop.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;
import org.msgpack.type.Value;

class MessagePackEncoder extends MessageToByteEncoder<Value> {

    private final MessagePack _msgpack;

    public MessagePackEncoder(final MessagePack msgpack){
        _msgpack = msgpack;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Value msg, ByteBuf out) throws Exception {

        _msgpack.createPacker(new ByteBufOutputStream(out)).write(msg);

        ctx.flush();
    }
}