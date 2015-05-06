package org.msgpack.rpc.impl.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.msgpack.rpc.transport.RpcMessageHandler;

import com.fasterxml.jackson.databind.node.ArrayNode;

class MessageHandler extends ChannelInboundHandlerAdapter {

    private final RpcMessageHandler rpcHandler;

    public MessageHandler(final RpcMessageHandler rpcHandler) {
        this.rpcHandler = rpcHandler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        rpcHandler.handleMessage(new ChannelAdaptor(ctx.channel()), (ArrayNode) msg);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
