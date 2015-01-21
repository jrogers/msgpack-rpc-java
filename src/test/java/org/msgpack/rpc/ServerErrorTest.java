package org.msgpack.rpc;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.msgpack.rpc.error.RemoteError;
import org.msgpack.rpc.loop.EventLoop;

/**
 * Test when server throws exceptions.
 * User: takeshita
 * Create: 12/06/15 12:12
 */
public class ServerErrorTest {

    public static class TestServer{

        @SuppressWarnings("unused")
        public String echo(String message){
            return message;
        }

        @SuppressWarnings("unused")
        public String waitWhile(int waitMSecs) throws Exception{
            Thread.sleep(waitMSecs);
            return "ok";
        }

        @SuppressWarnings("unused")
        public String throwException(String errorMessage) throws Exception{
            throw new Exception(errorMessage);
        }

        @SuppressWarnings("unused")
        public String throwRuntimeException(String errorMessage) {
            throw new RuntimeException(errorMessage);
        }
    }

    interface CallFunc {
        void apply(Client client);
    }

    void call(CallFunc func)  throws Exception{
        EventLoop loop = EventLoop.start();
        Server svr = new Server(loop);
        Client c = new Client("127.0.0.1", 19850, loop);
        c.setRequestTimeout(1);
        try {
            svr.serve(new TestServer());
            svr.listen(19850);

            func.apply(c);

        } finally {
            svr.close();
            c.close();
            loop.shutdown();
        }
    }

    @Test
    public void normalException()  throws Exception {
        call(new CallFunc(){
            public void apply(Client client) {
                String message = "Normal exception";
                try {
                    client.callApply("throwException", message);
                    fail("Must throw exception");
                } catch (RemoteError e) {
                    assertEquals(message, e.getMessage());
                } catch (Exception e) {
                    System.out.println(e.getClass());
                    fail("Not normal exception");
                }
            }
        });
    }

    @Test
    public void runtimeException()  throws Exception {
        call(new CallFunc(){
            public void apply(Client client) {
                String message = "Normal exception";
                try {
                    client.callApply("throwRuntimeException", message);
                    fail("Must throw exception");
                } catch (RemoteError e) {
                    assertEquals(message, e.getMessage());
                } catch (Exception e) {
                    System.out.println(e.getClass());
                    fail("Not normal exception");
                }
            }
        });
    }

    @Test
    public void errorMessage()  throws Exception {
        call(new CallFunc(){
            public void apply(Client client) {
                try {
                    client.callApply("throwRuntimeException", new Object[] { null });
                    fail("Must throw exception");
                } catch (RemoteError e) {
                    assertEquals("", e.getMessage());
                } catch (Exception e) {
                    System.out.println(e.getClass());
                    fail("Not normal exception");
                }
            }
        });
    }

    @Test
    public void noMethodError()  throws Exception {
        call(new CallFunc(){
            public void apply(Client client) {
                try {
                    client.callApply("methodWhichDoesNotExist", new Object[] { null });
                    fail("Must throw exception");
                } catch (RemoteError e) {
                    assertEquals(".CallError.NoMethodError", e.getMessage());
                } catch (Exception e) {
                    System.out.println("Wrong exception:" + e.getClass());
                    fail("Not NoMethodException");
                }
            }
        });
    }

    @Test
    public void badArgs()  throws Exception {
        call(new CallFunc(){
            public void apply(Client client) {
                try {
                    client.callApply("echo"/*, 1*/);
                    fail("Must throw exception");
                } catch (RemoteError e) {
                    // OK
                } catch (Exception e) {
                    System.out.println("Wrong exception:" + e.getClass());
                    fail("Not NoMethodException");
                }
            }
        });
    }

    @Test
    public void timeout()  throws Exception {
        call(new CallFunc(){
            public void apply(Client client) {
                try {
                    client.callApply("waitWhile", 3000);
                    fail("Must throw exception");
                } catch (RemoteError e) {
                    assertEquals("timedout", e.getMessage());
                } catch (Exception e) {
                    System.out.println("Wrong exception:" + e.getClass());
                    fail("Not NoMethodException");
                }
            }
        });
    }
}
