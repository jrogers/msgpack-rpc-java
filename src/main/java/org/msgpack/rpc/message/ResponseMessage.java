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
package org.msgpack.rpc.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class ResponseMessage implements Message {
    private int msgId;
    private Object error;
    private Object result;

    public ResponseMessage(int msgId, Object error, Object result) {
        this.msgId = msgId;
        this.error = error;
        this.result = result;
    }

    public ArrayNode toObjectArray(ObjectMapper mapper) {
        ArrayNode messageNode = mapper.createArrayNode();
        messageNode.add(Messages.RESPONSE);
        messageNode.add(msgId);
        messageNode.addPOJO(error);
        messageNode.addPOJO(result);
        return messageNode;
    }
}
