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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.msgpack.rpc.message.Message;
import org.msgpack.rpc.Session;
import org.msgpack.rpc.config.TcpClientConfig;
import org.msgpack.rpc.transport.ClientTransport;
import org.msgpack.rpc.transport.RpcMessageHandler;

class NettyTcpClientTransport implements ClientTransport {

    private final Session _session;
    private final Bootstrap _bootstrap;
    private final AtomicInteger _availables = new AtomicInteger(1024);
    private final ConcurrentLinkedQueue<Channel> _writables;

    NettyTcpClientTransport(final TcpClientConfig config,
                            final Session session,
                            final NettyEventLoop loop) {

        // TODO check session.getAddress() instanceof IPAddress
        final RpcMessageHandler handler = new RpcMessageHandler(session);

        _bootstrap = new Bootstrap()
            .group(new NioEventLoopGroup(/*2*/))
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.round((float) config.getConnectTimeout()))
            .option(ChannelOption.TCP_NODELAY, !Boolean.FALSE.equals(config.getOption(ChannelOption.TCP_NODELAY.name())))
            .option(ChannelOption.SO_KEEPALIVE, !Boolean.FALSE.equals(config.getOption(ChannelOption.SO_KEEPALIVE.name())))
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(
                            new MessagePackDecoder(loop.getObjectMapper()),
                            new MessageHandler(handler),
                            new MessagePackEncoder(loop.getObjectMapper()));
                }
            });

        _session = session;
        _writables = new ConcurrentLinkedQueue<Channel>();
    }

    protected ChannelFuture startConnection() {

        return _bootstrap.connect(_session.getAddress().getSocketAddress());
    }

    public void sendMessage(final Message msg) {

        if(_writables.isEmpty() && _availables.getAndDecrement() > 0){

            startConnection().addListener(new ChannelFutureListener() {

                public void operationComplete(ChannelFuture future) throws Exception {

                    final Channel connected = future.channel();

                    sendMessageChannel(connected, msg);

                    connected.closeFuture().addListener(new ChannelFutureListener() {
                        public void operationComplete(ChannelFuture channelFuture) throws Exception {
                            _availables.incrementAndGet();
                        }
                    });
                }
            });
        }
        else{

            final Channel writable = _writables.poll();

            if(writable != null){

                sendMessageChannel(writable, msg);
            }
            else{

                Thread.yield();
                sendMessage(msg);
            }
        }
    }

    public void close(){

        while(!_writables.isEmpty()){
               _writables.poll().close();
        }

    }

    protected ChannelFuture sendMessageChannel(Channel c, Object msg) {

        //System.out.println("[client transport] send message");

        return c.writeAndFlush(msg).addListener(new ChannelFutureListener() {

            public void operationComplete(ChannelFuture future) throws Exception {

                //System.out.println("[client transport] message sent!!!");

                _writables.offer(future.channel());
            }
        });
    }
}
