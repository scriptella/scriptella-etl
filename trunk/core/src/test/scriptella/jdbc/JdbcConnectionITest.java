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
package scriptella.jdbc;

import scriptella.DBTestCase;
import scriptella.execution.EtlExecutorException;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Integration test for JDBC connection.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JdbcConnectionITest extends DBTestCase {
    /**
     * Integration test for {@link scriptella.jdbc.JdbcConnection}. This testcase focused on Batching feature.
     *
     * @throws EtlExecutorException
     */
    public void test() throws EtlExecutorException {
        Connection c = getConnection("jdbcconitest");
        newEtlExecutor().execute();
        //Verify results1 for batch execution
        final List<String> results = new ArrayList<String>();
        QueryHelper q = new QueryHelper("SELECT * FROM BatchTestResults");
        q.execute(c, new QueryCallback() {
            public void processRow(ParametersCallback parameters) {
                results.add(parameters.getParameter("1").toString());
            }
        });
        assertEquals("Table BatchTestResults records count", 2, results.size());
        assertEquals("Result1: Query should return 5, because one batch with size 5 has been sent", "5", results.get(0));
        assertEquals("Result2: Query should return 15, 3 batches with size 5 has been sent", "15", results.get(1));

        //Now verify that pending inserts were flushed correctly
        results.clear();
        q = new QueryHelper("SELECT COUNT(*) FROM BatchTest WHERE ID=1");
        q.execute(c, new QueryCallback() {
            public void processRow(ParametersCallback parameters) {
                results.add(parameters.getParameter("1").toString());
            }
        });
        assertEquals("6", results.get(0));
        q = new QueryHelper("SELECT COUNT(*) FROM BatchTest WHERE ID=2");
        q.execute(c, new QueryCallback() {
            public void processRow(ParametersCallback parameters) {
                results.add(parameters.getParameter("1").toString());
            }
        });
        assertEquals("16", results.get(1));
    }
}
