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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

class FutureImpl {
    private final Session session;
    private Runnable callback = null;

    private final Object lock = new Object();
    private int timeout;
    private volatile boolean done = false;

    private JsonNode result;
    private JsonNode error;

    FutureImpl(Session session) {
        this.session = session;
        this.timeout = session.getRequestTimeout();
    }

    void attachCallback(Runnable callback) {
        boolean was_already_done;
        synchronized (lock) {
            was_already_done = done;
            if (!done) {
                this.callback = callback;
            }
        }
        if (was_already_done) {
            session.getEventLoop().getWorkerExecutor().submit(callback);
        }
    }

    void join() throws InterruptedException {
        synchronized (lock) {
            while (!done) {
                lock.wait();
            }
        }
    }

    void join(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        long end_time = System.currentTimeMillis() + unit.toMillis(timeout);
        boolean run_callback = false;
        synchronized (lock) {
            while (!done) {
                long timeout_remaining = end_time - System.currentTimeMillis();
                if (timeout_remaining <= 0) break;
                lock.wait(timeout_remaining);
            }

            if (!done) {
                ArrayNode error = session.getEventLoop().getObjectMapper().createArrayNode();
                error.add("timedout");
                this.error = error;
                done = true;
                lock.notifyAll();
                run_callback = true;
            }
        }
        if (run_callback && callback != null) {
            // FIXME #SF submit?
            // session.getEventLoop().getWorkerExecutor().submit(callback);
            callback.run();
        }
    }

    public boolean isDone() {
        return done;
    }

    public JsonNode getResult() {
        return result;
    }

    public JsonNode getError() {
        return error;
    }

    public void setResult(JsonNode result, JsonNode error) {
        synchronized (lock) {
            if (done) {
                return;
            }

            this.result = result;
            this.error = error;
            this.done = true;
            lock.notifyAll();
        }
        if (callback != null) {
            // FIXME #SF submit?
            // session.getEventLoop().getWorkerExecutor().submit(callback);
            callback.run();
        }
    }

    boolean stepTimeout() {
        if (timeout <= 0) {
            return true;
        } else {
            timeout--;
            return false;
        }
    }
}
