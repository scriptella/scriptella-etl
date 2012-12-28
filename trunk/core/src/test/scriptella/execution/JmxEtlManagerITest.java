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
import scriptella.interactive.ProgressIndicator;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Set;


/**
 * Integration test for {@link JmxEtlManager}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JmxEtlManagerITest extends AbstractTestCase {
    public void test() throws EtlExecutorException, MalformedObjectNameException {
        final EtlExecutor e = newEtlExecutor();
        e.setJmxEnabled(true);

        final ObjectName name = JmxEtlManager.toObjectName(e.getConfiguration().getDocumentUrl().toString(), 0);
        final MBeanServer srv = ManagementFactory.getPlatformMBeanServer();
        final long started = System.currentTimeMillis();
        e.execute(new ProgressIndicator() {
            public void showProgress(final double progress, final String message) {
                if (progress==1) { //oncomplete
                    //MBean is still present
                    final Set set = srv.queryMBeans(name, null);
                    assertEquals(1, set.size());

                    try {
                        final Number n = (Number) srv.getAttribute(name, "ExecutedStatementsCount");
                        assertEquals(2, n.intValue());
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                    try {
                        final Date d = (Date) srv.getAttribute(name, "StartDate");
                        assertTrue(d.getTime()>=started && d.getTime()<=System.currentTimeMillis());
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                    try {
                        final Number n = (Number) srv.getAttribute(name, "Throughput");
                        assertTrue(n.doubleValue()>0);
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }



                }
            }
        });
        //Mbean should be unregistered
        assertFalse(srv.isRegistered(name));

    }



}
