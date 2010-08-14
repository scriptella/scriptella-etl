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
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

/**
 * Tests for {@link StatementWrapper}.
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class StatementWrapperTest extends DBTestCase {
    public void testBatchedPrepared() throws IOException, EtlExecutorException, SQLException {
        Connection c = getConnection("stmtw");
        EtlExecutor ex = newEtlExecutor();
        ex.execute();
        PreparedStatement ps = c.prepareStatement("INSERT INTO Test VALUES (?)");
        StatementWrapper.BatchedPrepared batchedPrepared = new StatementWrapper.BatchedPrepared(ps, new JdbcTypesConverter(), 5);
        batchedPrepared.setParameters(Collections.<Object>singletonList(1));
        batchedPrepared.update();
        batchedPrepared.setParameters(Collections.<Object>singletonList(2));
        batchedPrepared.update();
        batchedPrepared.setParameters(Collections.<Object>singletonList(3));
        batchedPrepared.update();
        QueryHelper q = new QueryHelper("SELECT COUNT(*) FROM Test");
        final String[] r = new String[1];

        q.execute(c, new QueryCallback() {
            public void processRow(ParametersCallback parameters) {
                r[0] = parameters.getParameter("1").toString();
            }
        });
        assertEquals("Table should contain only initial record", "1", r[0]);

        int n = batchedPrepared.flush();
        assertEquals("3 modified rows should be reported", 3, n);
        q.execute(c, new QueryCallback() {
            public void processRow(ParametersCallback parameters) {
                r[0] = parameters.getParameter("1").toString();
            }
        });
        assertEquals("Table should contain 4 records(1 initial and 3 from batch)", "4", r[0]);

        for (int i=0;i<5;i++) {
            batchedPrepared.setParameters(Collections.<Object>singletonList(3));
            batchedPrepared.update();
        }
        q.execute(c, new QueryCallback() {
            public void processRow(ParametersCallback parameters) {
                r[0] = parameters.getParameter("1").toString();
            }
        });
        assertEquals("Table should contain 9 records", "9", r[0]);
    }

    public void testBatchedSimple() throws IOException, EtlExecutorException, SQLException {
        Connection c = getConnection("stmtw");
        EtlExecutor ex = newEtlExecutor();
        ex.execute();
        Statement s = c.createStatement();
        StatementWrapper.Batched batched = new StatementWrapper.Batched(s, new JdbcTypesConverter(), 3);
        batched.setSql("INSERT INTO Test VALUES (1)");
        batched.update();
        batched.clear();//calling this method should not break the batch sequence
        batched.setSql("INSERT INTO Test VALUES   (2)");
        batched.update();
        batched.setSql("INSERT INTO Test VALUES  (3)");
        batched.update();
        batched.setSql("INSERT INTO Test VALUES   (4)   ");
        batched.update();
        batched.setSql("INSERT INTO Test VALUES   (5)  ");
        batched.update();


        QueryHelper q = new QueryHelper("SELECT COUNT(*) FROM Test");
        final String[] r = new String[1];

        q.execute(c, new QueryCallback() {
            public void processRow(ParametersCallback parameters) {
                r[0] = parameters.getParameter("1").toString();
            }
        });
        assertEquals("Table should contain 4 records", "4", r[0]);

        int n = batched.flush();
        assertEquals("2 modified rows should be reported", 2, n);
        q.execute(c, new QueryCallback() {
            public void processRow(ParametersCallback parameters) {
                r[0] = parameters.getParameter("1").toString();
            }
        });
        assertEquals("Table should contain 6 records(1 initial and 5 from batch)", "6", r[0]);

    }


}