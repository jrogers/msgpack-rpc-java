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

import org.msgpack.rpc.*;

import java.util.*;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DispatcherTest extends ReflectTest {
	@Test
	public void syncHandler() throws Exception {
		Context context = startServer(new SyncHandler());
		Client c = context.getClient();
		try {
			String result;

			result = c.callApply("m01", String.class);
			assertEquals("m01", result);

			result = c.callApply("m02", String.class, "furuhashi");
			assertEquals("m02" + "furuhashi", result);

			result = c.callApply("m03", String.class, 1978);
			assertEquals("m03" + 1978, result);

			List<String> list = new ArrayList<>();
			list.add("sadayuki");
			list.add("kumofs");
			result = c.callApply("m04", String.class, list);
			assertEquals("m04" + stringify1(list), result);

			List<List<String>> alist = new ArrayList<>();
			List<String> alist_n1 = new ArrayList<>();
			alist_n1.add("1");
			alist_n1.add("2");
			alist_n1.add("3");
			alist.add(alist_n1);
			List<String> alist_n2 = new ArrayList<>();
			alist_n2.add("a");
			alist_n2.add("b");
			alist_n2.add("c");
			alist.add(alist_n2);
			result = c.callApply("m05", String.class, alist);
			assertEquals("m05"+stringify2(alist), result);

			result = c.callApply("m06", String.class, "viver", 2006);
			assertEquals("m06" + "viver" + 2006, result);

		} finally {
			context.close();
		}
	}

    @Test
	public void asyncHandler() throws Exception {
		Context context = startServer(new AsyncHandler());
		Client c = context.getClient();
		try {
			String result;

			result = c.callApply("m01", String.class);
			assertEquals("m01", result);

			result = c.callApply("m02", String.class, "furuhashi");
			assertEquals("m02" + "furuhashi", result);

			result = c.callApply("m03", String.class, 1978);
			assertEquals("m03" + 1978, result);

			List<String> list = new ArrayList<>();
			list.add("sadayuki");
			list.add("kumofs");
			result = c.callApply("m04", String.class, list);
			assertEquals("m04" + stringify1(list), result);

			List<List<String>> alist = new ArrayList<>();
			List<String> alist_n1 = new ArrayList<>();
			alist_n1.add("1");
			alist_n1.add("2");
			alist_n1.add("3");
			alist.add(alist_n1);
			List<String> alist_n2 = new ArrayList<>();
			alist_n2.add("a");
			alist_n2.add("b");
			alist_n2.add("c");
			alist.add(alist_n2);
			result = c.callApply("m05", String.class, alist);
			assertEquals("m05" + stringify2(alist), result);

			result = c.callApply("m06", String.class, "viver", 2006);
			assertEquals("m06" + "viver" + 2006, result);

		} finally {
			context.close();
		}
	}
}

