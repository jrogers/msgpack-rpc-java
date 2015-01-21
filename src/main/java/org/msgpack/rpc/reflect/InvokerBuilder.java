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

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.*;
import java.lang.annotation.*;

import org.msgpack.rpc.Callback;
import org.msgpack.rpc.annotation.Ignore;
import org.msgpack.rpc.annotation.Index;
import org.msgpack.rpc.annotation.NotNullable;
import org.msgpack.rpc.annotation.Optional;

public abstract class InvokerBuilder {
    public static class ArgumentEntry {
        private int index;
        private Type genericType;
        private FieldOption option;

        public ArgumentEntry() {
            this.index = -1;
            this.genericType = null;
            this.option = FieldOption.IGNORE;
        }

        public ArgumentEntry(ArgumentEntry e) {
            this.index = e.index;
            this.genericType = e.genericType;
            this.option = e.option;
        }

        public ArgumentEntry(int index, Type genericType, FieldOption option) {
            this.index = index;
            this.genericType = genericType;
            this.option = option;
        }

        public int getIndex() {
            return index;
        }

        public Class<?> getType() {
            if (genericType instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) genericType)
                        .getRawType();
            } else {
                return (Class<?>) genericType;
            }
        }

        public String getJavaTypeName() {
            Class<?> type = getType();
            if (type.isArray()) {
                return arrayTypeToString(type);
            } else {
                return type.getName();
            }
        }

        public Type getGenericType() {
            return genericType;
        }

        public FieldOption getOption() {
            return option;
        }

        public boolean isAvailable() {
            return option != FieldOption.IGNORE;
        }

        public boolean isRequired() {
            return option == FieldOption.NOTNULLABLE;
        }

        public boolean isOptional() {
            return option == FieldOption.OPTIONAL;
        }

        public boolean isNullable() {
            return option != FieldOption.NOTNULLABLE;
        }

        static String arrayTypeToString(Class<?> type) {
            int dim = 1;
            Class<?> baseType = type.getComponentType();
            while (baseType.isArray()) {
                baseType = baseType.getComponentType();
                dim += 1;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(baseType.getName());
            for (int i = 0; i < dim; ++i) {
                sb.append("[]");
            }
            return sb.toString();
        }
    }

    // Override this method
    public abstract Invoker buildInvoker(Method targetMethod,
                                         ArgumentEntry[] entries, boolean async);

    public Invoker buildInvoker(Method targetMethod) {
        checkValidation(targetMethod);
        boolean async = isAsyncMethod(targetMethod);
        return buildInvoker(targetMethod,
                readArgumentEntries(targetMethod, async), async);
    }

    // TODO ArgumentList を作る ArgumentOptionSet が必要
    // TODO FieldList を作る FieldOptionSet

    static boolean isAsyncMethod(Method targetMethod) {
        final Class<?>[] types = targetMethod.getParameterTypes();
        return types.length > 0 && Callback.class.isAssignableFrom(types[0]);
    }

    private static void checkValidation(Method targetMethod) {
        // TODO
    }

    static ArgumentEntry[] readArgumentEntries(Method targetMethod, boolean async) {
        Type[] types = targetMethod.getGenericParameterTypes();
        Annotation[][] annotations = targetMethod.getParameterAnnotations();

        int paramsOffset = 0;
        if (async) {
            paramsOffset = 1;
        }

        /*
         * index:
         *
         * @Index(0) int field_a; // 0
         * int field_b; // 1
         *
         * @Index(3) int field_c; // 3
         * int field_d; // 4
         *
         * @Index(2) int field_e; // 2
         * int field_f; // 5
         */
        List<ArgumentEntry> indexed = new ArrayList<>();
        int maxIndex = -1;
        for (int i = paramsOffset; i < types.length; i++) {
            Type t = types[i];
            Annotation[] as = annotations[i];

            FieldOption opt = readFieldOption(t, as);
            if (opt == FieldOption.IGNORE) {
                // skip
                continue;
            }

            int index = readFieldIndex(as, maxIndex);

            if (indexed.size() > index && indexed.get(index) != null) {
                // FIXME exception
                throw new IllegalArgumentException("duplicated index: " + index);
            }
            if (index < 0) {
                // FIXME exception
                throw new IllegalArgumentException("invalid index: " + index);
            }

            while (indexed.size() <= index) {
                indexed.add(null);
            }
            indexed.set(index, new ArgumentEntry(i, t, opt));

            if (maxIndex < index) {
                maxIndex = index;
            }
        }

        ArgumentEntry[] result = new ArgumentEntry[maxIndex + 1];
        for (int i = 0; i < indexed.size(); i++) {
            ArgumentEntry e = indexed.get(i);
            if (e == null) {
                result[i] = new ArgumentEntry();
            } else {
                result[i] = e;
            }
        }

        return result;
    }

    private static FieldOption readFieldOption(Type type, Annotation[] as) {
        if (isAnnotated(as, Ignore.class)) {
            return FieldOption.IGNORE;
        } else if (isAnnotated(as, NotNullable.class)) {
            return FieldOption.NOTNULLABLE;
        } else if (isAnnotated(as, Optional.class)) {
            return FieldOption.OPTIONAL;
        } else {
            if (type instanceof Class<?> && ((Class<?>) type).isPrimitive()) {
                return FieldOption.NOTNULLABLE;
            } else {
                return FieldOption.DEFAULT;
            }
        }
    }

    private static int readFieldIndex(Annotation[] as, int maxIndex) {
        Index a = getAnnotation(as, Index.class);
        if (a == null) {
            return maxIndex + 1;
        } else {
            return a.value();
        }
    }

    private static boolean isAnnotated(Annotation[] array,
                                       Class<? extends Annotation> with) {
        return getAnnotation(array, with) != null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Annotation> T getAnnotation(Annotation[] array,
                                                          Class<T> key) {
        for (Annotation a : array) {
            if (key.isInstance(a)) {
                return (T) a;
            }
        }
        return null;
    }
}
