package org.msgpack.rpc.impl.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.msgpack.rpc.transport.RpcMessageHandler;
import org.msgpack.type.Value;

class MessageHandler extends ChannelInboundHandlerAdapter {

    private final RpcMessageHandler _rpcHandler;

    public MessageHandler(final RpcMessageHandler rpcHandler){
        _rpcHandler = rpcHandler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        _rpcHandler.handleMessage(new ChannelAdaptor(ctx.channel()), (Value) msg);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, Throwable cause) throws Exception {

        cause.printStackTrace();
        ctx.close();
    }
}
