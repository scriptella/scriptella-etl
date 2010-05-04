/*
 * Copyright 2006-2009 The Scriptella Project Team.
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
import scriptella.jdbc.QueryHelper;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

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
}
