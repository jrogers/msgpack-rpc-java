package org.msgpack.rpc.builder;

import org.msgpack.rpc.dispatcher.Dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * User: takeshita
 * Create: 12/06/15 0:51
 */
public interface DispatcherBuilder {

    public Dispatcher build(Object handler, ObjectMapper mapper) ;
}
