/*
 * Copyright 2006 The Scriptella Project Team.
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

/**
 * Tests for {@link JmxEtlManager}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JmxEtlManagerTest extends AbstractTestCase {
    public void testRegistration() throws MalformedURLException, MalformedObjectNameException {
        EtlContext ctx = new EtlContext();
        ctx.setBaseURL(new URL("file:/tmp"));
        JmxEtlManager m = new JmxEtlManager(ctx);
        m.register();
        final ObjectName name = new ObjectName("scriptella:type=etl,url=\"file:/tmp\"");
        assertTrue(ManagementFactory.getPlatformMBeanServer().isRegistered(name));
        m.unregister();
        assertFalse(ManagementFactory.getPlatformMBeanServer().isRegistered(name));
        //Now test naming collision
        m = new JmxEtlManager(ctx);
        m.register();
        JmxEtlManager m2 = new JmxEtlManager(ctx);
        m2.register();
        final ObjectName name2 = new ObjectName("scriptella:type=etl,url=\"file:/tmp\",n=1");
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

}
