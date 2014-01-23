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

import java.util.concurrent.ConcurrentLinkedQueue;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.msgpack.rpc.Session;
import org.msgpack.rpc.config.TcpClientConfig;
import org.msgpack.rpc.transport.ClientTransport;
import org.msgpack.rpc.transport.RpcMessageHandler;

class NettyTcpClientTransport implements ClientTransport {

    private final Session _session;
    private final Bootstrap bootstrap;
    private final ConcurrentLinkedQueue<Channel> _channels;

    NettyTcpClientTransport(final TcpClientConfig config,
                            final Session session,
                            final NettyEventLoop loop) {

        // TODO check session.getAddress() instanceof IPAddress
        final RpcMessageHandler handler = new RpcMessageHandler(session);

        bootstrap = new Bootstrap(); // (1)
        bootstrap.group(new NioEventLoopGroup(2)); // (2)
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
}
