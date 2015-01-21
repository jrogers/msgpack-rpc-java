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
package org.msgpack.rpc;

import org.msgpack.rpc.message.ResponseMessage;
import org.msgpack.rpc.transport.MessageSendable;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class Request implements Callback<Object> {

    private MessageSendable channel; // TODO #SF synchronized?
    private int msgId;
    private String method;
    private ArrayNode args;

    public Request(MessageSendable channel, int msgId, String method, ArrayNode args) {
        this.channel = channel;
        this.msgId = msgId;
        this.method = method;
        this.args = args;
    }

    public Request(String method, ArrayNode args) {
        this.channel = null;
        this.msgId = 0;
        this.method = method;
        this.args = args;
    }

    public String getMethodName() {
        return method;
    }

    public ArrayNode getArguments() {
        return args;
    }

    public int getMessageID() {
        return msgId;
    }

    public void sendResult(Object result) {
        sendResponse(result, null);
    }

    public void sendError(Object error) {
        sendResponse(null, error);
    }

    public void sendError(Object error, Object data) {
        sendResponse(data, error);
    }

    public synchronized void sendResponse(Object result, Object error) {
        if (channel == null) {
            return;
        }

        ResponseMessage msg = new ResponseMessage(msgId, error, result);
        channel.sendMessage(msg);
        channel = null;
    }
}
