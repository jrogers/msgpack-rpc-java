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
import java.nio.ByteBuffer;
import java.util.List;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;
import org.msgpack.MessagePackable;
import org.msgpack.rpc.Server;
import org.msgpack.rpc.config.TcpServerConfig;
import org.msgpack.rpc.transport.RpcMessageHandler;
import org.msgpack.rpc.transport.ServerTransport;
import org.msgpack.rpc.address.Address;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Unpacker;

class NettyTcpServerTransport implements ServerTransport {

    private ChannelFuture channelFuture;

    NettyTcpServerTransport(final TcpServerConfig config,
                            final Server server,
                            final NettyEventLoop loop) throws InterruptedException {

        if (server == null) {
            throw new IllegalArgumentException("Server must not be null");
        }

        Address address = config.getListenAddress();

        final RpcMessageHandler handler = new RpcMessageHandler(server);
        handler.useThread(true);

        final EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        final ServerBootstrap b = new ServerBootstrap(); // (2)
        b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class) // (3)
            .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(
                            new MessagePackDecoder(loop.getMessagePack()),
                            new MessageHandler(handler),
                            new MessagePackEncoder(loop.getMessagePack()),
                            new MessagePackableEncoder(loop.getMessagePack()));
                }
            })
            .option(ChannelOption.SO_BACKLOG, 128)          // (5)
            .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

        // Bind and start to accept incoming connections.
        channelFuture = b.bind(address.getSocketAddress()).sync(); // (7)
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

            //System.out.println("[server transport] got bytebuf to decode");

            final ByteBuffer buffer = byteBuf.markReaderIndex().nioBuffer().slice();

            try{
                Unpacker unpacker = _msgpack.createBufferUnpacker(buffer);

                out.add(unpacker.readValue());

                //System.out.println("[server transport] feed value to the next");

                byteBuf.skipBytes(buffer.position());
            }
            catch( EOFException e ){

                //System.out.println("[server transport] not enough bytebuf");

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

            //System.out.println("[server transport] encoding msg of Value");

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

            //System.out.println("[server transport] encoding msg of Value from packable encoder");

            msg.writeTo(_msgpack.createPacker(new ByteBufOutputStream(out)));

            //System.out.println("[server transport] encoding msg of Value from packable encoder [written]");
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

            //System.out.println("[server transport] got message decoded: " + msg.getClass().getName());

            Value value = (Value) msg;

            _rpcHandler.handleMessage(new ChannelAdaptor(ctx.channel()), value);
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, Throwable cause) throws Exception {

            cause.printStackTrace();
            ctx.close();
        }
    }

    public void close() {
        channelFuture.channel().close();
    }
}
