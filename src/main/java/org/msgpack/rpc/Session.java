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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.msgpack.rpc.address.Address;
import org.msgpack.rpc.message.RequestMessage;
import org.msgpack.rpc.message.NotifyMessage;
import org.msgpack.rpc.reflect.Reflect;
import org.msgpack.rpc.transport.ClientTransport;
import org.msgpack.rpc.config.ClientConfig;
import org.msgpack.rpc.loop.EventLoop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class Session {
    protected Address address;
    protected EventLoop loop;
    private ClientTransport transport;
    private Reflect reflect;

    private int requestTimeout;
    private AtomicInteger seqid = new AtomicInteger(0); // FIXME rand()?
    private Map<Integer, FutureImpl> reqtable = new HashMap<>();

    Session(Address address, ClientConfig config, EventLoop loop) {
        this(address, config, loop, new Reflect(loop.getObjectMapper()));
    }

    Session(Address address, ClientConfig config, EventLoop loop, Reflect reflect) {
        this.address = address;
        this.loop = loop;
        this.requestTimeout = config.getRequestTimeout();
        this.transport = loop.openTransport(config, this);
        this.reflect = reflect;
    }

    public <T> T proxy(Class<T> iface) {
        return reflect.getProxy(iface).newProxyInstance(this);
    }

    public Address getAddress() {
        return address;
    }

    // FIXME EventLoopHolder interface?
    public EventLoop getEventLoop() {
        return loop;
    }

    /**
     * Timeout seconds
     * @return
     */
    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public JsonNode callApply(String method, Object... args) {
        Future<JsonNode> f = sendRequest(method, null, args);
        while (true) {
            try {
                if (requestTimeout <= 0){
                    return f.get();
                } else {
                    return f.get(requestTimeout, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                // FIXME
            } catch (TimeoutException e) {
                // FIXME
                throw new RuntimeException("Time out to call method:" + method,e);
            }
        }
    }

    public <V> V callApply(final String method, final Class<V> resultClass, final Object... args) {
        JavaType resultType = loop.getObjectMapper().constructType(resultClass);
        Future<V> f = sendRequest(method, resultType, args);
        while (true) {
            try {
                if (requestTimeout <= 0){
                    return f.get();
                } else {
                    return f.get(requestTimeout, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                // FIXME
            } catch (TimeoutException e) {
                // FIXME
                throw new RuntimeException("Time out to call method:" + method, e);
            }
        }
    }

    public <V> V callApply(final String method, final TypeReference<V> resultTypeReference, final Object... args) {
        JavaType resultType = loop.getObjectMapper().getTypeFactory().constructType(resultTypeReference);
        Future<V> f = sendRequest(method, resultType, args);
        while (true) {
            try {
                if (requestTimeout <= 0){
                    return f.get();
                } else {
                    return f.get(requestTimeout, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                // FIXME
            } catch (TimeoutException e) {
                // FIXME
                throw new RuntimeException("Time out to call method:" + method,e);
            }
        }
    }

    public Future<JsonNode> callAsyncApply(final String method, final Object... args) {
        return sendRequest(method, null, args);
    }

    public <T> Future<T> callAsyncApply(final String method, final Class<T> resultClass,
                                        final Object... args) {
        JavaType resultType = loop.getObjectMapper().constructType(resultClass);
        return sendRequest(method, resultType, args);
    }

    public <T> Future<T> callAsyncApply(final String method, final TypeReference<T> resultTypeReference,
                                        final Object... args) {
        JavaType resultType = loop.getObjectMapper().getTypeFactory().constructType(resultTypeReference);
        return sendRequest(method, resultType, args);
    }

    public void notifyApply(final String method, final Object... args) {
        sendNotify(method, args);
    }

    private <T> Future<T> sendRequest(String method, JavaType resultType, Object[] args) {
        int msgId = seqid.getAndAdd(1);
        ArrayNode requestArgs = buildArgumentsArray(args);
        RequestMessage msg = new RequestMessage(msgId, method, requestArgs);
        FutureImpl f = new FutureImpl(this);

        synchronized (reqtable) {
            reqtable.put(msgId, f);
        }

        transport.sendMessage(msg);

        return new Future<>(loop.getObjectMapper(), f, resultType);
    }

    private void sendNotify(String method, Object[] args) {
        ArrayNode notifyArgs = buildArgumentsArray(args);
        NotifyMessage msg = new NotifyMessage(method, notifyArgs);
        transport.sendMessage(msg);
    }

    private ArrayNode buildArgumentsArray(Object[] args) {
        // Build the arguments array and serialize the arguments
        final ArrayNode argArray = loop.getObjectMapper().createArrayNode();
        if (args != null) {
            for (Object arg : args) {
                argArray.addPOJO(arg);
            }
        }

        return argArray;
    }

    void closeSession() {
        transport.close();
        synchronized (reqtable) {
            for (Map.Entry<Integer, FutureImpl> pair : reqtable.entrySet()) {
                // FIXME error result
                FutureImpl f = pair.getValue();
                ArrayNode arrayNode = loop.getObjectMapper().createArrayNode();
                arrayNode.add("session closed");
                f.setResult(null, arrayNode);
            }
            reqtable.clear();
        }
    }

    public void transportConnectFailed() { // FIXME error rseult
        /*
        synchronized(reqtable) {
            for(Map.Entry<Integer,FutureImpl> pair : reqtable.entrySet()) {
                // FIXME
                FutureImpl f = pair.getValue();
                f.setResult(null,null);
            }
            reqtable.clear();
        }
        */
    }

    public void onResponse(int msgid, JsonNode result, JsonNode error) {
        FutureImpl f;
        synchronized (reqtable) {
            f = reqtable.remove(msgid);
        }
        if (f == null) {
            // FIXME log
            return;
        }
        f.setResult(result, error);
    }

    void stepTimeout() {
        List<FutureImpl> timedout = new ArrayList<>();
        synchronized (reqtable) {
            for (Iterator<Map.Entry<Integer, FutureImpl>> it = reqtable
                    .entrySet().iterator(); it.hasNext();) {
                Map.Entry<Integer, FutureImpl> pair = it.next();
                FutureImpl f = pair.getValue();
                if (f.stepTimeout()) {
                    it.remove();
                    timedout.add(f);
                }
            }
        }
        for (FutureImpl f : timedout) {
            // FIXME error result
            ArrayNode arrayNode = loop.getObjectMapper().createArrayNode();
            arrayNode.add("timedout");
            f.setResult(null, arrayNode);
        }
    }
}
