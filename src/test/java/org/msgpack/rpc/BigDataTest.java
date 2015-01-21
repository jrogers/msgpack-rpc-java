package org.msgpack.rpc;

import org.msgpack.rpc.dispatcher.*;
import org.msgpack.rpc.loop.*;

import java.util.*;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class BigDataTest {
	
    private static String getBigString() {
        StringBuilder sb = new StringBuilder(1024); // 1M
        Random random = new Random();
        for(int i = 0; i < 1024; i++) {
            sb.append((char)('a' + random.nextInt(26)));
        }
        return sb.toString();
    }

    private static String BIG_DATA = getBigString();

    public static class BigDataDispatcher implements Dispatcher {

        public void dispatch(Request request) {

            assertEquals(BIG_DATA, request.getArguments().get(0).asText());

			request.sendResult(BIG_DATA);
		}
	}

	@Test
	public void syncBigDataLoad() throws Exception {
		EventLoop loop = EventLoop.start();
		Server svr = new Server(loop);
		Client c = new Client("127.0.0.1", 19851, loop);
		c.setRequestTimeout(100);

		try {
			svr.serve(new BigDataDispatcher());
			svr.listen(19851);

            //warmup
            assertEquals(BIG_DATA, c.callApply("test", String.class, BIG_DATA));

			int num = 10;

			long start = System.currentTimeMillis();
			for (int i = 0; i < num; i++) {
				assertEquals(BIG_DATA, c.callApply("test", String.class, BIG_DATA));
			}

			long finish = System.currentTimeMillis();

			double result = num / ((double)(finish - start) / 1000);
			System.out.printf("sync: %f calls per sec, and avg: %fms per call", result, (double) (finish - start) / num);

		}
        finally {
			svr.close();
			c.close();
			loop.shutdown();
		}
    }

    @Test
	public void asyncBigDataLoad() throws Exception {
		EventLoop loop = EventLoop.start();
		Server svr = new Server(loop);
		Client c = new Client("127.0.0.1", 19852, loop);
		c.setRequestTimeout(100);//

		try {
			svr.serve(new BigDataDispatcher());
			svr.listen(19852);

            //warmup
            assertEquals(BIG_DATA, c.callApply("test", String.class, BIG_DATA));

			int num = 10000;

			long start = System.currentTimeMillis();
			for (int i = 0; i < num - 1; i++) {
				c.notifyApply("test", BIG_DATA);
			}

			c.callApply("test", BIG_DATA);
			long finish = System.currentTimeMillis();

			double result = num / ((double)(finish - start) / 1000);
			System.out.println("async: "+result+" calls per sec");

		}
        finally {
			svr.close();
			c.close();
			loop.shutdown();
		}
	}
}
