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
package scriptella;

import scriptella.configuration.ConfigurationEl;
import scriptella.configuration.ConnectionEl;
import scriptella.core.ConnectionManager;
import scriptella.core.SqlTestHelper;
import scriptella.execution.EtlContext;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;
import scriptella.execution.TestableEtlExecutor;
import scriptella.jdbc.QueryHelper;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.sql.Connection;
import java.util.List;
import java.util.Map;


/**
 * This class provides several tests for checking transaction attributes processing.
 */
public class TxTest extends DBTestCase {
    public void test() {
        final Connection con = getConnection("txtest");
        final EtlExecutor se = newEtlExecutor("TxTest.xml");

        try {
            se.execute();
        } catch (EtlExecutorException e) {
            e.printStackTrace();
            fail("Scripts invoked in new tx must not fail the executor");
        }

        QueryHelper s = new QueryHelper("select * from test");
        final int n[] = new int[]{0};

        s.execute(con,
                new QueryCallback() {
                    public void processRow(final ParametersCallback evaluator) {
                        n[0]++;
                        assertEquals(n[0], evaluator.getParameter("ID"));
                    }
                });
        assertEquals(n[0], 3);
    }

    public void test2() {
        final java.sql.Connection con = getConnection("txtest2");
        final EtlExecutor se = newEtlExecutor("TxTest2.xml");

        try {
            se.execute();
        } catch (EtlExecutorException e) {
            e.printStackTrace();
            fail("Scripts invoked in new tx must not fail the executor");
        }

        QueryHelper s = new QueryHelper("select * from test");
        final int n[] = new int[]{0};

        s.execute(con,
                new QueryCallback() {
                    public void processRow(final ParametersCallback row) {
                        n[0]++;
                        assertEquals(n[0], row.getParameter("ID"));
                    }
                });
        assertEquals(1, n[0]);
    }

    /**
     * This test case validates newtx handling, i.e. only one extra connection should be opened for
     * scripts with newtx=true attribute
     */
    public void test3() {
        final Connection con = getConnection("txtest3");
        ConfigurationEl conf = loadConfiguration("TxTest3.xml");
        final String failed[] = new String[1];
        final EtlExecutor se = new TestableEtlExecutor(conf) {
            @Override
            public void rollbackAll(final EtlContext ctx) {
                failed[0] = "Script should not be rolled back";
                super.rollbackAll(ctx);
            }

            @Override
            public void closeAll(final EtlContext ctx) {
                final Map<String, ConnectionManager> connections = SqlTestHelper.getConnections(ctx.getSession());
                final ConnectionManager cf = connections.get(ConnectionEl.DEFAULT_ID);
                final List<scriptella.spi.Connection> newConnections = SqlTestHelper.getNewConnections(cf);

                if ((newConnections == null) ||
                        (newConnections.size() != 1)) {
                    failed[0] = "Only one connection should be created for newtx script";
                }

                if (SqlTestHelper.getConnection(cf) == null) {
                    failed[0] = "Connection should be initialized";
                }

                super.closeAll(ctx);
            }
        };

        try {
            se.execute();
        } catch (EtlExecutorException e) {
            e.printStackTrace();
            fail("Scripts invoked in new tx must not fail the executor: " +
                    e.getMessage());
        }

        if (failed[0] != null) {
            fail(failed[0]);
        }

        QueryHelper s = new QueryHelper("select * from test2");

        s.execute(con,
                new QueryCallback() {
                    public void processRow(final ParametersCallback row) {
                        fail("Table Test2 should have no rows");
                    }
                });
    }
}
