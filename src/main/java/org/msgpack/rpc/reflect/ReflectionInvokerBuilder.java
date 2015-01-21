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
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.lang.reflect.*;

import org.msgpack.rpc.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReflectionInvokerBuilder extends InvokerBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionInvokerBuilder.class);
    protected ObjectMapper mapper;

    public ReflectionInvokerBuilder(ObjectMapper mapper){
        this.mapper = mapper;
    }


    static abstract class ReflectionArgumentEntry extends ArgumentEntry {
        ReflectionArgumentEntry(ArgumentEntry e) {
            super(e);
        }

        public abstract void convert(Object[] params, JsonNode obj) throws IllegalArgumentException;

        public void setNull(Object[] params) {
            params[getIndex()] = null;
        }
    }

    static class NullArgumentEntry extends ReflectionArgumentEntry {
        NullArgumentEntry(ArgumentEntry e) {
            super(e);
        }
        public void convert(Object[] params, JsonNode obj) throws IllegalArgumentException {
            if (obj.isNull()) {
                setNull(params);
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    static class BooleanArgumentEntry extends ReflectionArgumentEntry {
        BooleanArgumentEntry(ArgumentEntry e) {
            super(e);
        }
        public void convert(Object[] params, JsonNode obj) throws IllegalArgumentException {
            if (obj.isBoolean()) {
                params[getIndex()] = obj.booleanValue();
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    static class ByteArgumentEntry extends ReflectionArgumentEntry {
        ByteArgumentEntry(ArgumentEntry e) {
            super(e);
        }
        public void convert(Object[] params, JsonNode obj) throws IllegalArgumentException {
            if (obj.isInt()) { // FIXME: can we do better?
                params[getIndex()] = obj.intValue();
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    static class ShortArgumentEntry extends ReflectionArgumentEntry {
        ShortArgumentEntry(ArgumentEntry e) {
            super(e);
        }
        public void convert(Object[] params, JsonNode obj) throws IllegalArgumentException {
            if (obj.isShort()) {
                params[getIndex()] = obj.shortValue();
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    static class IntArgumentEntry extends ReflectionArgumentEntry {
        IntArgumentEntry(ArgumentEntry e) {
            super(e);
        }
        public void convert(Object[] params, JsonNode obj) throws IllegalArgumentException {
            if (obj.isInt()) {
                params[getIndex()] = obj.intValue();
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    static class LongArgumentEntry extends ReflectionArgumentEntry {
        LongArgumentEntry(ArgumentEntry e) {
            super(e);
        }
        public void convert(Object[] params, JsonNode obj) throws IllegalArgumentException {
            if (obj.isLong()) {
                params[getIndex()] = obj.longValue();
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    static class FloatArgumentEntry extends ReflectionArgumentEntry {
        FloatArgumentEntry(ArgumentEntry e) {
            super(e);
        }
        public void convert(Object[] params, JsonNode obj) throws IllegalArgumentException {
            if (obj.isFloat()) {
                params[getIndex()] = obj.floatValue();
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    static class DoubleArgumentEntry extends ReflectionArgumentEntry {
        DoubleArgumentEntry(ArgumentEntry e) {
            super(e);
        }
        public void convert(Object[] params, JsonNode obj) throws IllegalArgumentException {
            if (obj.isDouble()) {
                params[getIndex()] = obj.doubleValue();
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    static class ObjectArgumentEntry extends ReflectionArgumentEntry {

        private ObjectMapper mapper;
        private JavaType type;

        ObjectArgumentEntry(ArgumentEntry e, ObjectMapper mapper, Type genericType) {
            super(e);
            this.mapper = mapper;
            type = mapper.constructType(genericType);
        }

        @SuppressWarnings("unchecked")
        public void convert(Object[] params, JsonNode obj) throws IllegalArgumentException {
            params[getIndex()] = mapper.convertValue(obj, type);
        }
    }

    private static class ReflectionInvoker implements Invoker {
        protected Method method;
        protected int parameterLength;
        protected ReflectionArgumentEntry[] entries;
        protected int minimumArrayLength;
        boolean async;

        public ReflectionInvoker(Method method, ReflectionArgumentEntry[] entries, boolean async) {
            this.method = method;
            this.parameterLength = method.getParameterTypes().length;
            this.entries = entries;
            this.async = async;
            this.minimumArrayLength = 0;
            for(int i=0; i < entries.length; i++) {
                ReflectionArgumentEntry e = entries[i];
                if(!e.isOptional()){//e.isRequired() || e.isNullable()) {
                    this.minimumArrayLength = i+1;
                }
            }
        }

        public void invoke(Object target, Request request) throws Exception {
            Object[] params = new Object[parameterLength];
            if (async) {
                params[0] = request;
            }

            // TODO set default values here

            try {
                ArrayNode args = request.getArguments();
                int length = args.size();
                if (length < minimumArrayLength) {
                    throw new IllegalArgumentException(String.format("Method needs at least %s args.But only %s args are passed", minimumArrayLength, length));
                }

                int i;
                for (i = 0; i < minimumArrayLength; i++) {
                    ReflectionArgumentEntry e = entries[i];
                    if (!e.isAvailable()) {
                        continue;
                    }

                    JsonNode obj = args.get(i);
                    if (obj.isNull()) {
                        if (e.isRequired()) {
                            // Required + nil => exception
                            throw new IllegalArgumentException();
                        } else if (e.isOptional()) {
                            // Optional + nil => keep default value
                        } else {  // Nullable
                            // Nullable + nil => set null
                            e.setNull(params);
                        }
                    } else {
                        try {
                            e.convert(params, obj);
                        } catch (IllegalArgumentException mte){
                            logger.error(String.format("Expect Method:%s ArgIndex:%s Type:%s. But passed:%s", request.getMethodName(), i, e.getGenericType(), obj));
                            throw new IllegalArgumentException(String.format(
                                    "%sth argument type is %s.But wrong type is sent.", i + 1, e.getJavaTypeName())
                            );
                        }
                    }
                }

                int max = length < entries.length ? length : entries.length;
                for (; i < max; i++) {
                    ReflectionArgumentEntry e = entries[i];
                    if (!e.isAvailable()) {
                        continue;
                    }

                    JsonNode obj = args.get(i);
                    if (obj.isNull()) {
                        // this is Optional field becaue i >= minimumArrayLength
                        // Optional + nil => keep default value
                    } else {
                        try {
                            e.convert(params, obj);
                        } catch (IllegalArgumentException iae){
                            logger.error(String.format("Expect Method:%s ArgIndex:%s Type:%s. But passed:%s", request.getMethodName(), i, e.getGenericType(), obj));
                            throw new IllegalArgumentException(String.format(
                                    "%sth argument type is %s.But wrong type is sent.", i + 1, e.getJavaTypeName())
                            );
                        }
                    }
                }

                // latter entries are all Optional + nil => keep default value
            } catch (Exception e) {
                //e.printStackTrace();
                throw e;
            }

            Object result;
            try {
                result = method.invoke(target, params);
            } catch (InvocationTargetException e ){
                if (e.getCause() != null && e.getCause() instanceof Exception){
                    throw (Exception)e.getCause();
                } else{
                    throw e;
                }
            }

            if (!async) {
                request.sendResult(result);
            }
            // TODO exception
        }
    }

    public Invoker buildInvoker(Method targetMethod, ArgumentEntry[] entries, boolean async) {
        int mod = targetMethod.getModifiers();
        if(!Modifier.isPublic(mod)) {
            targetMethod.setAccessible(true);
        }

        ReflectionArgumentEntry[] res = new ReflectionArgumentEntry[entries.length];
        for (int i = 0; i < entries.length; i++) {
            ArgumentEntry e = entries[i];
            Class<?> type = e.getType();
            if (!e.isAvailable()) {
                res[i] = new NullArgumentEntry(e);
            } else if (type.equals(boolean.class)) {
                res[i] = new BooleanArgumentEntry(e);
            } else if (type.equals(byte.class)) {
                res[i] = new ByteArgumentEntry(e);
            } else if (type.equals(short.class)) {
                res[i] = new ShortArgumentEntry(e);
            } else if (type.equals(int.class)) {
                res[i] = new IntArgumentEntry(e);
            } else if (type.equals(long.class)) {
                res[i] = new LongArgumentEntry(e);
            } else if (type.equals(float.class)) {
                res[i] = new FloatArgumentEntry(e);
            } else if (type.equals(double.class)) {
                res[i] = new DoubleArgumentEntry(e);
            } else {
                res[i] = new ObjectArgumentEntry(e, mapper, e.getGenericType());
            }
        }

        return new ReflectionInvoker(targetMethod, res, async);
    }
}

