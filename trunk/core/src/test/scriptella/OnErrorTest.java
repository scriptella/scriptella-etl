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
package scriptella;

import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;
import scriptella.jdbc.JdbcException;
import scriptella.jdbc.QueryHelper;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Tests support of onerror elements.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class OnErrorTest extends DBTestCase {
    public void test() throws EtlExecutorException {
        final Connection con = getConnection("onerrortest");
        EtlExecutor se = newEtlExecutor();
        se.execute();
        QueryHelper q = new QueryHelper("select * from test");
        final Map<Integer, String> expected = new LinkedHashMap<Integer, String>();
        expected.put(1, "Updated1");
        expected.put(2, "Updated2");
        expected.put(3, "Updated3");
        expected.put(4, "444");
        expected.put(5, "555");
        q.execute(con, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                Integer id = (Integer) parameters.getParameter("id");
                assertEquals(expected.get(id), parameters.getParameter("value"));
                expected.remove(id);
            }
        });
        assertTrue(expected.isEmpty());

    }

    /**
     * CRQ-54586 Feature request: Allow different connection-id in ononerror element
     * @throws EtlExecutorException
     */
    public void testCRQ54586() throws EtlExecutorException {
        final Connection con = getConnection("onerrortest2");
        EtlExecutor se = newEtlExecutor(getClass().getSimpleName()+"2.xml");
        se.execute();
        QueryHelper q = new QueryHelper("select * from error_log");
        final int[] cnt = new int[1];
        q.execute(con, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                String msg = (String) parameters.getParameter("msg");
                assertTrue(msg.startsWith("Error occurred"));
                assertTrue("Error message should contain JdbcException", msg.contains(JdbcException.class.getName()));
                cnt[0]++;
            }
        });
        assertEquals("One error should be logged", 1, cnt[0]);
        getConnection("onerrortest"); //call getConnection simply to shutdown HSQLDB after the test
    }

    /**
     * BUG-193124 Error during script execution causes an infinite loop for an onerror handler with retry enabled
     */
    public void testRetry() throws EtlExecutorException, InterruptedException {
      ExecutorService es = Executors.newFixedThreadPool(1);

      EtlExecutor etlExecutor = newEtlExecutor(getClass().getSimpleName()+"3.xml");
      es.submit((Runnable) etlExecutor);
      es.shutdown();
      es.awaitTermination(1, TimeUnit.SECONDS);
      if (!es.isTerminated()) {
        es.shutdownNow();
        fail(etlExecutor + " should be terminated, but is still running.");
      }
    }

}
