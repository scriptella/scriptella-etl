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

import scriptella.execution.ScriptsExecutor;
import scriptella.execution.ScriptsExecutorException;

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
public class SQLSupportPerformanceTest extends DBTestCase {
    private static final byte SQL[] = "update ${'test'} set id=?{property};rollback;".getBytes();
    private static final byte SQL2[] = "update test set id=?{property};".getBytes();

    /**
     * 5406
     * 5563
     */
    public void test() throws ScriptsExecutorException {
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

        ScriptsExecutor se = newScriptsExecutor();
        se.execute();
    }

    /**
     * 3703 (2281,875)
     * 3843 (2312,938)
     * @throws ScriptsExecutorException
     * @throws SQLException
     * @throws IOException
     */
    public void testCompare() throws ScriptsExecutorException, SQLException, IOException {
        final int n = 20000;
        Connection con = getConnection("sqlsupport");
        con.setAutoCommit(false);
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

        ScriptsExecutor se = newScriptsExecutor();
        long ti = System.currentTimeMillis();
        se.execute();
        ti = System.currentTimeMillis()-ti;
        System.out.println("ti = " + ti);
        //Now let's test direct HSQL connection
        RepeatingInputStream ris = new RepeatingInputStream("update test set id=?\n".getBytes(), n);
        BufferedReader br = new BufferedReader(new InputStreamReader(ris));
        ti = System.currentTimeMillis();
        for (String s;(s=br.readLine())!=null;) {
            PreparedStatement ps = con.prepareStatement(s);
            ps.setObject(1,1);
            ps.execute();
            ps.close();
        }
        con.commit();
        ti = System.currentTimeMillis()-ti;
        System.out.println("ti hsql = " + ti);



    }


    public static void main(final String args[])
            throws ScriptsExecutorException {
        SQLSupportPerformanceTest t = new SQLSupportPerformanceTest();
        t.test();
    }

    private static class RepeatingInputStream extends InputStream {
        private int count;
        private byte data[];
        private int pos;

        public RepeatingInputStream(byte data[], int count) {
            this.data = data;
            this.count = count;
        }

        public int read() throws IOException {
            if (pos >= data.length) {
                count--;

                if (count > 0) {
                    pos = 0;
                } else {
                    return -1;
                }
            }

            int r = data[pos] & 0xff;
            pos++;

            return r;
        }
    }
}
