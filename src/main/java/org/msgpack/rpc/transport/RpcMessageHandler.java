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
package org.msgpack.rpc.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.msgpack.rpc.message.Messages;
import org.msgpack.rpc.Session;
import org.msgpack.rpc.Server;
import org.msgpack.rpc.loop.EventLoop;

public class RpcMessageHandler {

    protected final Session session;
    protected final Server server;
    protected final EventLoop loop;
    protected boolean useThread = false;

    public RpcMessageHandler(Session session) {
        this(session, null);
    }

    public RpcMessageHandler(Server server) {
        this(null, server);
    }

    public RpcMessageHandler(Session session, Server server) {
        this.session = session;
        this.server = server;
        if (session == null) {
            this.loop = server.getEventLoop();
        }
        else {
            this.loop = session.getEventLoop();
        }
    }

    public void useThread(boolean value) {
        useThread = value;
    }

    static class HandleMessageTask implements Runnable {
        private RpcMessageHandler handler;
        private MessageSendable channel;
        private ArrayNode msg;

        HandleMessageTask(RpcMessageHandler handler, MessageSendable channel, ArrayNode msg) {
            this.handler = handler;
            this.channel = channel;
            this.msg = msg;
        }

        public void run() {
            handler.handleMessageImpl(channel, msg);
        }
    }

    public void handleMessage(MessageSendable channel, ArrayNode msg) {
        if (useThread) {
            loop.getWorkerExecutor().submit(
                    new HandleMessageTask(this, channel, msg));
        } else {
            handleMessageImpl(channel, msg);
        }
    }

    private void handleMessageImpl(MessageSendable channel, ArrayNode msg) {

        // TODO check array.length
        int type = msg.get(0).asInt();
        if (type == Messages.REQUEST) {
            // REQUEST
            int msgId = msg.get(1).asInt();
            String method = msg.get(2).asText();
            ArrayNode args = (ArrayNode) msg.get(3);
            handleRequest(channel, msgId, method, args);

        }
        else if (type == Messages.RESPONSE) {
            // RESPONSE
            int msgId = msg.get(1).asInt();
            JsonNode error = msg.get(2);
            JsonNode result = msg.get(3);
            handleResponse(channel, msgId, result, error);

        }
        else if (type == Messages.NOTIFY) {
            // NOTIFY
            String method = msg.get(1).asText();
            ArrayNode args = (ArrayNode) msg.get(2);
            handleNotify(channel, method, args);

        }
        else {
            // FIXME error result
            throw new RuntimeException("unknown message type: " + type);
        }
    }

    private void handleRequest(MessageSendable channel, int msgId,
            String method, ArrayNode args) {
        if (server == null) {
            return; // FIXME error result
        }
        server.onRequest(channel, msgId, method, args);
    }

    private void handleNotify(MessageSendable channel, String method, ArrayNode args) {
        if (server == null) {
            return; // FIXME error result?
        }
        server.onNotify(method, args);
    }

    private void handleResponse(MessageSendable channel, int msgId,
            JsonNode result, JsonNode error) {
        if (session == null) {
            return; // FIXME error?
        }
        session.onResponse(msgId, result, error);
    }
}
