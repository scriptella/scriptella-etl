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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SQLSupportPerformanceTest extends DBTestCase {
    private static final byte SQL[] = "update ${'test'} set id=?{property};rollback;".getBytes();

    public void test() throws ScriptsExecutorException {
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
