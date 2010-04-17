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
package scriptella;

import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;
import scriptella.util.RepeatingInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SQLSupportPerfTest extends DBTestCase {
    private static final byte SQL[] = "update ${'test'} set id=?{property};rollback;".getBytes();
    private static final byte SQL2[] = "update test set id=?{property};".getBytes();
    private static final byte SQL3[] = "update test set id=12345;".getBytes();

    /**
     * History:
     * 04.11.2006 - Duron 1.7Mhz - 1400 ms
     * 11.09.2006 - Duron 1.7Mhz - 1578 ms
     */
    public void test() throws EtlExecutorException {
        getConnection("sqlsupport");
        AbstractTestCase.testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                return new RepeatingInputStream(SQL, 50000);
            }

            public OutputStream getOutputStream(final URL u) {
                throw new UnsupportedOperationException();
            }

            public int getContentLength(final URL u) {
                return 50000 * SQL.length;
            }
        };

        EtlExecutor se = newEtlExecutor();
        se.execute();
    }

    /**
     * History:
     * 04.11.2006 - Duron 1.7Mhz - 2300 ms
     * 11.09.2006 - Duron 1.7Mhz - 2578 ms
     *
     * @throws EtlExecutorException
     * @throws SQLException
     * @throws IOException
     */
    public void testCompare() throws EtlExecutorException, SQLException, IOException {
        final int n = 20000;
        Connection con = getConnection("sqlsupport");
        AbstractTestCase.testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                return new RepeatingInputStream(SQL2, n);
            }

            public OutputStream getOutputStream(final URL u) {
                throw new UnsupportedOperationException();
            }

            public int getContentLength(final URL u) {

                return n * SQL.length;
            }
        };

        EtlExecutor se = newEtlExecutor();
        long ti = System.currentTimeMillis();
        se.execute();
        ti = System.currentTimeMillis() - ti;
        System.out.println("ti = " + ti);
        //Now let's test direct HSQL connection
        RepeatingInputStream ris = new RepeatingInputStream("update test set id=?\n".getBytes(), n);
        BufferedReader br = new BufferedReader(new InputStreamReader(ris));
        ti = System.currentTimeMillis();
        for (String s; (s = br.readLine()) != null;) {
            PreparedStatement ps = con.prepareStatement(s);
            ps.setObject(1, 1);
            ps.execute();
            ps.close();
        }
        con.commit();
        ti = System.currentTimeMillis() - ti;
        System.out.println("ti hsql = " + ti);


    }


    /**
     * History:
     * 19.01.2007 - Duron 1.7Mhz - 330 ms
     *
     * @throws EtlExecutorException
     * @throws SQLException
     * @throws IOException
     */
    public void testBulkUpdates() throws EtlExecutorException {
        //50000 identical statements
        getConnection("sqlsupport");
        AbstractTestCase.testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                return new RepeatingInputStream(SQL3, 50000);
            }

            public OutputStream getOutputStream(final URL u) {
                throw new UnsupportedOperationException();
            }

            public int getContentLength(final URL u) {
                return 50000 * SQL3.length;
            }
        };

        EtlExecutor se = newEtlExecutor();
        se.execute();
    }


    public static void main(final String args[]) throws EtlExecutorException {
        SQLSupportPerfTest t = new SQLSupportPerfTest();
        t.test();
    }

}
