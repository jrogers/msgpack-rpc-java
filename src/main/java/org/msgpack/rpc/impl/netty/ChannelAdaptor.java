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

import io.netty.channel.Channel;

import org.msgpack.rpc.message.Message;
import org.msgpack.rpc.transport.ClientTransport;

class ChannelAdaptor implements ClientTransport {

    private final Channel _channel;

    protected ChannelAdaptor(final Channel channel) {
        _channel = channel;
    }

    public void sendMessage(Message msg) {
        _channel.writeAndFlush(msg);
    }

    public void close() {
        _channel.close();
    }
}
