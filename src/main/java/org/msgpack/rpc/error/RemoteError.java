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
package org.msgpack.rpc.error;

import com.fasterxml.jackson.databind.JsonNode;

public class RemoteError extends RPCError {

    private JsonNode data;

    public RemoteError(JsonNode data) {
        super(loadMessage(data));
        this.data = data;
    }

    public JsonNode getData() {
        return data;
    }

    private static String loadMessage(JsonNode data) {
        if (data.isTextual()) {
            return data.asText();
        } else if (data.size() > 0) {
            return data.get(0).asText();
        }

        return null;
    }

    public static final String CODE = "RemoteError";

    @Override
    public String getCode() {
        return CODE;
    }
}
