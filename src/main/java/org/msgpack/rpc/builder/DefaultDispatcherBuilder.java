package org.msgpack.rpc.builder;

import org.msgpack.rpc.dispatcher.Dispatcher;
import org.msgpack.rpc.dispatcher.MethodDispatcher;
import org.msgpack.rpc.reflect.Reflect;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * User: takeshita
 * Create: 12/06/15 1:16
 */
public class DefaultDispatcherBuilder implements DispatcherBuilder {

    public Dispatcher build(Object handler, ObjectMapper mapper) {
        return new MethodDispatcher(
                new Reflect(mapper), handler);
    }
}

