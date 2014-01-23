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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.msgpack.rpc.Server;
import org.msgpack.rpc.config.TcpServerConfig;
import org.msgpack.rpc.transport.RpcMessageHandler;
import org.msgpack.rpc.transport.ServerTransport;
import org.msgpack.rpc.address.Address;

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

    public void close() {
        channelFuture.channel().close();
    }
}
