/*
 * Copyright 2006-2012 The Scriptella Project Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scriptella.execution;

import scriptella.AbstractTestCase;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for {@link JmxEtlManager}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JmxEtlManagerTest extends AbstractTestCase {

	public static final int NUMBER_OF_THREADS = 5;

	public void testRegistration() throws MalformedURLException, MalformedObjectNameException {
        EtlContext ctx = new EtlContext();
        ctx.setBaseURL(new URL("file:/tmp"));
        JmxEtlManager m = new JmxEtlManager(ctx);
        m.register();
        final ObjectName name = JmxEtlManager.toObjectName("file:/tmp", 0);
        assertTrue(ManagementFactory.getPlatformMBeanServer().isRegistered(name));
        m.unregister();
        assertFalse(ManagementFactory.getPlatformMBeanServer().isRegistered(name));
        //Now test naming collision
        m = new JmxEtlManager(ctx);
        m.register();
        JmxEtlManager m2 = new JmxEtlManager(ctx);
        m2.register();
        final ObjectName name2 = JmxEtlManager.toObjectName("file:/tmp", 1);
        assertTrue(ManagementFactory.getPlatformMBeanServer().isRegistered(name));
        assertTrue(ManagementFactory.getPlatformMBeanServer().isRegistered(name2));
        m.unregister();
        m2.unregister();
        assertFalse(ManagementFactory.getPlatformMBeanServer().isRegistered(name));
        assertFalse(ManagementFactory.getPlatformMBeanServer().isRegistered(name2));
        //Now test if double registration is prohibited
        m = new JmxEtlManager(ctx);
        m.register();
        try {
            m.register();
            fail("Double registration must be reported");
        } catch (IllegalStateException e) {
            //OK
        }
        m.unregister();
    }

    public void testCancelAll() throws MalformedURLException {
        EtlContext ctx = new EtlContext();
        ctx.setBaseURL(new URL("file:/tmp"));
        JmxEtlManager m = new JmxEtlManager(ctx);
        m.register();
        JmxEtlManager m2 = new JmxEtlManager(ctx);
        m2.register();
        ctx.setBaseURL(new URL("file:/tmp2"));
        JmxEtlManager m3 = new JmxEtlManager(ctx);
        m3.register();
        assertEquals(3,JmxEtlManager.cancelAll());
        m.unregister();
        m2.unregister();
        m3.unregister();
        //Check and clear the interrupted state
        assertTrue(Thread.interrupted());

    }

	/**
	 * Test for BUG-54489 InstanceNotFoundException when running multiple executors of the same script
	 */
	public void testParallelExecution() throws MalformedURLException, InterruptedException {
		final URL url = new URL("file:/tmp");
		final ExecutorService ex = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		final List<Throwable> exceptions = new CopyOnWriteArrayList<Throwable>();
		final AtomicInteger cnt = new AtomicInteger();
		for (int i = 0; i < NUMBER_OF_THREADS; i++) {
			ex.submit(new Runnable() {
				@Override
				public void run() {
					try {
						EtlContext ctx = new EtlContext();
						ctx.setBaseURL(url);
						JmxEtlManager m = new JmxEtlManager(ctx);
						m.register();
						assertTrue(ManagementFactory.getPlatformMBeanServer().isRegistered(m.getName()));
						m.unregister();
						cnt.incrementAndGet();
					} catch (Throwable e) {
						exceptions.add(e);
					}
				}
			});
		}
		ex.shutdown();
		ex.awaitTermination(1, TimeUnit.SECONDS);
		ex.shutdownNow();
		assertEquals("Errors occurred while executing in parallel", Collections.<Throwable> emptyList(),
				exceptions);
		assertEquals(NUMBER_OF_THREADS + " jobs are expected to complete", NUMBER_OF_THREADS, cnt.intValue());
		JmxEtlManager.cancelAll();
	}

}
