//
// MessagePack-RPC for Java
//
// Copyright (C) 2010 FURUHASHI Sadayuki
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package org.msgpack.rpc.loop.netty;

import java.io.EOFException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;
import org.msgpack.MessagePackable;
import org.msgpack.rpc.Session;
import org.msgpack.rpc.config.TcpClientConfig;
import org.msgpack.rpc.transport.ClientTransport;
import org.msgpack.rpc.transport.RpcMessageHandler;
import org.msgpack.rpc.transport.PooledStreamClientTransport;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Unpacker;

class NettyTcpClientTransport implements ClientTransport {

    private ByteBufOutputStream _bufOutput;

    private final Session _session;
    private final Bootstrap bootstrap;
    private final ConcurrentLinkedQueue<Channel> _channels;

    NettyTcpClientTransport(final TcpClientConfig config,
                            final Session session,
                            final NettyEventLoop loop) {

        // TODO check session.getAddress() instanceof IPAddress
        final RpcMessageHandler handler = new RpcMessageHandler(session);

        final EventLoopGroup workerGroup = new NioEventLoopGroup();

        bootstrap = new Bootstrap(); // (1)
        bootstrap.group(workerGroup); // (2)
        bootstrap.channel(NioSocketChannel.class); // (3)
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true); // (4)
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        new MessagePackDecoder(loop.getMessagePack()),
                        new MessageHandler(handler),
                        new MessagePackEncoder(loop.getMessagePack()),
                        new MessagePackableEncoder(loop.getMessagePack()));
            }
        });

        _session = session;
        _channels = new ConcurrentLinkedQueue<Channel>();
    }

    protected ChannelFuture startConnection() {
        return bootstrap.connect(_session.getAddress().getSocketAddress());
    }

    public void sendMessage(final Object msg) {

        if(_channels.isEmpty()){

            startConnection().addListener(new ChannelFutureListener() {

                public void operationComplete(ChannelFuture future) throws Exception {

                    sendMessageChannel(future.channel(), msg);
                }
            });
        }
        else{
            sendMessageChannel(_channels.poll(), msg);
        }
    }

    public void close(){

        System.out.println("[client transport] closing channels:" + _channels.size());

        while(!_channels.isEmpty()){
               _channels.poll().close();
        }

    }

    protected ChannelFuture sendMessageChannel(Channel c, Object msg) {

        //System.out.println("[client transport] send message");

        return c.writeAndFlush(msg).addListener(new ChannelFutureListener() {

            public void operationComplete(ChannelFuture future) throws Exception {

                //System.out.println("[client transport] message sent!!!");

                _channels.offer(future.channel());
            }
        });
    }

    static class MessagePackDecoder extends ByteToMessageDecoder {

        private final MessagePack _msgpack;

        public MessagePackDecoder(final MessagePack msgpack){
            _msgpack = msgpack;
        }

        @Override
        protected void decode(final ChannelHandlerContext channelHandlerContext,
                              final ByteBuf byteBuf,
                              final List<Object> out) throws Exception {

            //System.out.printf("[client transport] decode got bytebuf\n");

            final ByteBuffer buffer = byteBuf.markReaderIndex().nioBuffer().slice();

            try{
                Unpacker unpacker = _msgpack.createBufferUnpacker(buffer);
                out.add(unpacker.readValue());

                //System.out.printf("[client transport] decode done\n");

                byteBuf.skipBytes(buffer.position());
            }
            catch( EOFException e ){

                //System.out.println("[client transport] not enough bytebuf");

                byteBuf.resetReaderIndex();
            }
        }
    }

    static class MessagePackEncoder extends MessageToByteEncoder<Value> {

        private final MessagePack _msgpack;

        public MessagePackEncoder(final MessagePack msgpack){
            _msgpack = msgpack;
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, Value msg, ByteBuf out) throws Exception {

            //System.out.println("[client transport] encoding msg of Value");

            _msgpack.createPacker(new ByteBufOutputStream(out)).write(msg);

            ctx.flush();
        }
    }

    static class MessagePackableEncoder extends MessageToByteEncoder<MessagePackable> {

        private final MessagePack _msgpack;

        public MessagePackableEncoder(final MessagePack msgpack){
            _msgpack = msgpack;
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, MessagePackable msg, ByteBuf out) throws Exception {

            //System.out.println("[client transport] encoding msg of Value from packable encoder");

            msg.writeTo(_msgpack.createPacker(new ByteBufOutputStream(out)));
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

            super.write(ctx, msg, promise);

            ctx.flush();

            //System.out.println("[server transport] encoding msg of Value from packable encoder [flushed]");
        }
    }

    static class MessageHandler extends ChannelInboundHandlerAdapter {

        private final RpcMessageHandler _rpcHandler;

        public MessageHandler(final RpcMessageHandler rpcHandler){
            _rpcHandler = rpcHandler;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {

            //System.out.printf("[client transport] message handler got msg: " + msg.getClass().getName());

            Value value = (Value) msg;

            _rpcHandler.handleMessage(new ChannelAdaptor(ctx.channel()), value);
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, Throwable cause) throws Exception {

            cause.printStackTrace();
            ctx.close();
        }
    }
}
