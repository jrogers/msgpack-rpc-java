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
package org.msgpack.rpc.impl.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.msgpack.rpc.address.Address;
import org.msgpack.rpc.address.IPAddress;
import org.msgpack.rpc.Server;
import org.msgpack.rpc.config.TcpServerConfig;
import org.msgpack.rpc.transport.RpcMessageHandler;
import org.msgpack.rpc.transport.ServerTransport;
import org.msgpack.rpc.address.Address;

import java.net.InetSocketAddress;

class NettyTcpServerTransport implements ServerTransport {

    private final ChannelFuture channelFuture;

    NettyTcpServerTransport(final TcpServerConfig config,
                            final Server server,
                            final NettyEventLoop loop) throws InterruptedException {

        if (server == null) {
            throw new IllegalArgumentException("Server must not be null");
        }

        final Address address = config.getListenAddress();
        final RpcMessageHandler handler = new RpcMessageHandler(server);

        handler.useThread(true);

        final EventLoopGroup bossGroup = new NioEventLoopGroup(/*1*/); // (1)
        final EventLoopGroup workerGroup = new NioEventLoopGroup(/*4*/);
        final ServerBootstrap b = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class) // (3)
                .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new MessagePackDecoder(loop.getObjectMapper()),
                                new MessageHandler(handler),
                                new MessagePackEncoder(loop.getObjectMapper()));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                .childOption(ChannelOption.TCP_NODELAY, !Boolean.FALSE.equals(config.getOption(ChannelOption.TCP_NODELAY.name())))
                .childOption(ChannelOption.SO_KEEPALIVE, !Boolean.FALSE.equals(config.getOption(ChannelOption.SO_KEEPALIVE.name())));

        // Bind and start to accept incoming connections.
        channelFuture = b.bind(address.getSocketAddress()).sync(); // (7)
    }

    public Address getLocalAddress() {
        return new IPAddress((InetSocketAddress) channelFuture.channel().localAddress());
    }

    public void close() {
        channelFuture.channel().close();
    }
}
