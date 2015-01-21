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

import org.msgpack.rpc.dispatcher.*;
import org.msgpack.rpc.loop.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ServerTest {
	private static final String MESSAGE = "ok";
	public static class TestDispatcher implements Dispatcher {
		public void dispatch(Request request) {
			request.sendResult(MESSAGE);
		}
	}

	@Test
	public void syncLoad() throws Exception {
		EventLoop loop = EventLoop.start();
		Server svr = new Server(loop);
		Client c = new Client("127.0.0.1", 19850, loop);
		c.setRequestTimeout(10);

		try {
			svr.serve(new TestDispatcher());
			svr.listen(19850);

			int num = 1000;

			long start = System.currentTimeMillis();
			for (int i = 0; i < num; i++) {
				String result = c.callApply("test", String.class);
				assertEquals(MESSAGE, result);
			}

			long finish = System.currentTimeMillis();

			double result = num / ((double)(finish - start) / 1000);
			System.out.println("sync: "+result+" calls per sec");

		} finally {
			svr.close();
			c.close();
			loop.shutdown();
		}
	}

	@Test
	public void asyncLoad() throws Exception {
		EventLoop loop = EventLoop.start();
		Server svr = new Server(loop);
		Client c = new Client("127.0.0.1", 19850, loop);
		c.setRequestTimeout(100);//

		try {
			svr.serve(new TestDispatcher());
			svr.listen(19850);

			int num = 1000;

			long start = System.currentTimeMillis();
			for (int i = 0; i < num - 1; i++) {
				c.notifyApply("test");
			}

			c.callApply("test");
			long finish = System.currentTimeMillis();

			double result = num / ((double)(finish - start) / 1000);
			System.out.println("async: "+result+" calls per sec");
		} finally {
			svr.close();
			c.close();
			loop.shutdown();
		}
	}
}

