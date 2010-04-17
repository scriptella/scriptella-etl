/*
 * Copyright 2006-2007 The Scriptella Project Team.
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

import scriptella.DBTestCase;
import scriptella.jdbc.QueryHelper;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Tests if ETL cancellation(interruption) is working.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class CancellationTest extends DBTestCase {
    private boolean interrupted;

    public void test() throws EtlExecutorException {
        EtlExecutor etlExecutor = newEtlExecutor();
        Connection c = getConnection("cancelTest");
        final Thread etlThread = Thread.currentThread();
        interrupted = false;
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(200); //wait for ETL to start
                    etlThread.interrupt();
                    interrupted = true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.start();
        long ti = System.currentTimeMillis();
        try {
            etlExecutor.execute();
        } catch (EtlExecutorException e) {
            assertTrue(e.isCancelled());
        }
        ti = System.currentTimeMillis() - ti;
        assertTrue(interrupted);
        assertTrue(ti < 1000); //Long running ETL must be terminated ASAP
        //Now check if the tables were removed
        new QueryHelper("select count(*) from t1, t2") {
            @Override protected void onSQLException(SQLException e) {
                if (e.getMessage().indexOf("not found")<0) {
                    super.onSQLException(e);
                }

            }
        }.execute(c, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                assertEquals(0, parameters.getParameter("1"));
            }
        });
    }
}
