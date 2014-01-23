////
//// MessagePack-RPC for Java
////
//// Copyright (C) 2010 FURUHASHI Sadayuki
////
////    Licensed under the Apache License, Version 2.0 (the "License");
////    you may not use this file except in compliance with the License.
////    You may obtain a copy of the License at
////
////        http://www.apache.org/licenses/LICENSE-2.0
////
////    Unless required by applicable law or agreed to in writing, software
////    distributed under the License is distributed on an "AS IS" BASIS,
////    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
////    See the License for the specific language governing permissions and
////    limitations under the License.
////
//package org.msgpack.rpc.loop.netty;
//
//import java.io.ByteArrayInputStream;
//import java.io.EOFException;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.ByteBuffer;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import org.jboss.netty.channel.Channel;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.handler.codec.frame.FrameDecoder;
//import org.msgpack.MessagePack;
//import org.msgpack.type.Value;
//import org.msgpack.unpacker.Unpacker;
//
//public class MessagePackStreamDecoder extends FrameDecoder {
//    protected MessagePack msgpack;
//
//    public MessagePackStreamDecoder(MessagePack msgpack) {
//        super();
//        this.msgpack = msgpack;
//    }
//
//    @Override
//    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer source) throws Exception {
//
//        // TODO #MN will modify the body with MessagePackBufferUnpacker.
//        ByteBuffer buffer = source.toByteBuffer().slice();
//        if (!buffer.hasRemaining()) {
//            return null;
//        }
//        source.markReaderIndex();
//
//        try{
//            Unpacker unpacker = msgpack.createBufferUnpacker(buffer);
//            Value v = unpacker.readValue();
//            source.skipBytes(buffer.position());
//            return v;
//        }
//        catch( EOFException e ){
//            // not enough buffers.
//            // So retry reading
//            System.out.println("~~~waste~~~");
//            source.resetReaderIndex();
//            return null;
//        }
//    }
//}
