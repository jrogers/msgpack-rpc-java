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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.msgpack.rpc.error.RemoteError;

public class Future<V> implements java.util.concurrent.Future<V> {

    private ObjectMapper mapper;
    private FutureImpl impl;
    private JavaType resultType;

    Future(ObjectMapper mapper, FutureImpl impl) {
        this.mapper = mapper;
        this.impl = impl;
    }

    Future(ObjectMapper mapper, FutureImpl impl, JavaType resultType) {
        this.mapper = mapper;
        this.impl = impl;
        this.resultType = resultType;
    }

    public Future(ObjectMapper mapper, Future<JsonNode> future, JavaType resultType) {
        this(mapper, future.impl);
        this.resultType = resultType;
    }

    public void attachCallback(Runnable callback) {
        impl.attachCallback(callback);
    }

    public V get() throws InterruptedException {
        join();
        checkThrowError();
        return getResult();
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException,
            TimeoutException {
        join(timeout, unit);
        checkThrowError();
        return getResult();
    }

    public void join() throws InterruptedException {
        impl.join();
    }

    public void join(long timeout, TimeUnit unit) throws InterruptedException,
            TimeoutException {
        impl.join(timeout, unit);
    }

    public boolean isDone() {
        return impl.isDone();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        // FIXME
        return false;
    }

    public boolean isCancelled() {
        // FIXME
        return false;
    }

    @SuppressWarnings("unchecked")
    public V getResult() {
        JsonNode result = impl.getResult();
        if (resultType == null) {
            return (V) result;
        } else if (result.isNull()) {
            return null;
        } else {
            try {
                return mapper.convertValue(result, resultType);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public JsonNode getError() {
        return impl.getError();
    }

    private void checkThrowError() {
        if (!getError().isNull()) {
            // FIXME exception
            throw new RemoteError(getError());
        }
    }
}
