package org.msgpack.rpc;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.msgpack.rpc.builder.StopWatchDispatcherBuilder;
import org.msgpack.rpc.loop.EventLoop;

/**
 * User: takeshita
 * Create: 12/06/15 1:51
 */
public class DecoratorTest {

    public static class TestServer {

        @SuppressWarnings("unused")
        public String success() {
            return "ok";
        }

        @SuppressWarnings("unused")
        public String throwError(String errorMessage) throws Exception {
            throw new RuntimeException(errorMessage);
        }
    }

    /**
     * Test any Exception is not thrown.
     * @throws Exception
     */
    @Test
    public void decorateStopWatch()  throws Exception {
        EventLoop loop = EventLoop.start();
        Server svr = new Server(loop);
        svr.setDispatcherBuilder(
                new StopWatchDispatcherBuilder(svr.getDispatcherBuilder()).
                        withVerboseOutput(true)
        );

        Client c = new Client("127.0.0.1", 19850, loop);
        c.setRequestTimeout(10);
        try {
            svr.serve(new TestServer());
            svr.listen(19850);

            String result = c.callApply("success", String.class);

            assertNotNull(result);

            try {
                c.callApply("throwError", "StopWatchTest");
                fail("Exception must be thrown.");
            } catch (Exception e){
                // Catch the exception.
            }

        } finally {
            svr.close();
            c.close();
            loop.shutdown();
        }
    }
}
