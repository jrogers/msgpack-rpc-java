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
package org.msgpack.rpc.reflect;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.*;

import org.msgpack.rpc.*;

public class ReflectionProxyBuilder extends ProxyBuilder {

    private static class ReflectionMethodEntry {
        private String rpcName;
        private boolean async;
        private InvokerBuilder.ArgumentEntry[] argumentEntries;
        private JavaType returnType;

        public ReflectionMethodEntry(MethodEntry e, JavaType returnType) {
            this.rpcName = e.getRpcName();
            this.async = e.isAsync();
            this.argumentEntries = e.getArgumentEntries();
            this.returnType = returnType;
        }

        public String getRpcName() {
            return rpcName;
        }

        public boolean isAsync() {
            return async;
        }

        public Object[] sort(Object[] args) {
            Object[] params = new Object[argumentEntries.length];

            for(int i=0; i < argumentEntries.length; i++) {
                InvokerBuilder.ArgumentEntry e = argumentEntries[i];
                if(!e.isAvailable()) {
                    continue;
                }
                if(params.length < e.getIndex()) {
                    // FIXME
                }
                if(e.isRequired() && args[i] == null) {
                    // TODO type error
                }
                params[i] = args[e.getIndex()];
            }

            return params;
        }
    }

    public class ReflectionHandler implements InvocationHandler {
        private Session s;
        private Map<Method, ReflectionMethodEntry> entryMap;

        public ReflectionHandler(Session s, Map<Method, ReflectionMethodEntry> entryMap) {
            this.s = s;
            this.entryMap = entryMap;
        }

        @SuppressWarnings("unchecked")
        public Object invoke(Object proxy, Method method, Object[] args) {
            ReflectionMethodEntry e = entryMap.get(method);
            if (e == null) {
                // FIXME
            }

            Object[] params = e.sort(args);
            if (e.isAsync()) {
                Future<JsonNode> f = s.callAsyncApply(e.getRpcName(), params);
                return new Future<>(mapper, f, e.returnType);
            } else {
                JsonNode obj = s.callApply(e.getRpcName(), params);
                if (obj.isNull()) {
                    return null;
                } else {
                    try {
                        return mapper.convertValue(obj, e.returnType);
                    } catch (Exception ex) {
                        return null;
                    }
                }
            }
        }
    }

    public class ReflectionProxy<T> implements Proxy<T> {
        private Class<T> iface;
        private Map<Method, ReflectionMethodEntry> entryMap;

        public ReflectionProxy(Class<T> iface, Map<Method, ReflectionMethodEntry> entryMap) {
            this.iface = iface;
            this.entryMap = entryMap;
        }

        @SuppressWarnings("unchecked")
        public T newProxyInstance(Session s) {
            ReflectionHandler handler = new ReflectionHandler(s, entryMap);
            return (T)java.lang.reflect.Proxy.newProxyInstance(
                    iface.getClassLoader(), new Class[] { iface }, handler);
        }
    }

    private ObjectMapper mapper;

    public ReflectionProxyBuilder(ObjectMapper mapper){
        this.mapper = mapper;
    }

    public <T> Proxy<T> buildProxy(Class<T> iface, MethodEntry[] entries) {
        for (MethodEntry e : entries) {
            Method method = e.getMethod();
            int mod = method.getModifiers();
            if (!Modifier.isPublic(mod)) {
                method.setAccessible(true);
            }
        }

        Map<Method, ReflectionMethodEntry> entryMap = new HashMap<>();
        for(MethodEntry entry : entries) {
            entryMap.put(entry.getMethod(), new ReflectionMethodEntry(entry,
                    mapper.constructType(entry.getGenericReturnType())));
        }

        return new ReflectionProxy<T>(iface, entryMap);
    }
}

