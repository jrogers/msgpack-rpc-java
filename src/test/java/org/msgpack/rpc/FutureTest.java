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

import org.msgpack.rpc.loop.*;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FutureTest {
    public static class TestHandler {

        @SuppressWarnings("unused")
        public String m1(String a1) {
            return "ok" + a1;
        }

        @SuppressWarnings("unused")
        public String m2(Integer time_millis) throws InterruptedException {
            Thread.sleep(time_millis);
            return "ok" + time_millis;
        }
    }

    public interface TestInterface {
        Future<String> m1(String a1);

        Future<String> m1Async(String a1); // /Async$/ will be removed

        Future<String> m2(Integer time_millis);
    }

    @Test
    public void future() throws Exception {
        EventLoop loop = EventLoop.start();

        Server svr = new Server(loop);
        svr.serve(new TestHandler());
        svr.listen(19860);

        Client cli = new Client("127.0.0.1", 19860, loop);
        TestInterface c = cli.proxy(TestInterface.class);

        try {
            Future<String> f1 = c.m1("a1");
            Future<String> f2 = c.m1("a2");
            Future<String> f3 = c.m1Async("a3");
            Future<String> f4 = c.m2(5);
            Future<String> f5 = c.m2(60000);

            f3.join();
            f1.join();
            f2.join();
            f4.join(500, TimeUnit.MILLISECONDS);
            f5.join(500, TimeUnit.MILLISECONDS);

            assertEquals("ok" + "a1", f1.get());
            assertEquals("ok" + "a2", f2.get());
            assertEquals("ok" + "a3", f3.get());
            assertEquals("ok" + "5", f4.get());
            assertTrue(f4.getError().isNull());
            assertEquals("timedout", f5.getError().get(0).asText());

        } finally {
            svr.close();
            cli.close();
            loop.shutdown();
        }
    }
}
