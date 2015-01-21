package org.msgpack.rpc.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public interface Message {

    public ArrayNode toObjectArray(ObjectMapper mapper);
}
