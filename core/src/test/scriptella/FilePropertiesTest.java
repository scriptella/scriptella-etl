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
import scriptella.jdbc.QueryHelper;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.Arrays;


/**
 *
 */
public class FilePropertiesTest extends DBTestCase {
    private static final byte FILE[] = "test file/////".getBytes();

    public void test() throws EtlExecutorException {
        final Connection con = getConnection("fileproptst");
        AbstractTestCase.testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                return new ByteArrayInputStream(FILE);
            }

            public OutputStream getOutputStream(final URL u) {
                throw new UnsupportedOperationException();
            }

            public int getContentLength(final URL u) {
                return FILE.length;
            }
        };

        final EtlExecutor se = newEtlExecutor();
        se.execute();

        QueryHelper q = new QueryHelper("select (select count(id) from t), c from t");
        q.execute(con,
                new QueryCallback() {
                    public void processRow(final ParametersCallback rowEvaluator) {
                        final byte b[] = (byte[]) rowEvaluator.getParameter("c");
                        assertTrue(Arrays.equals(FILE, b));
                        assertEquals(3, rowEvaluator.getParameter("1")); //3 rows
                    }
                });
    }
}
