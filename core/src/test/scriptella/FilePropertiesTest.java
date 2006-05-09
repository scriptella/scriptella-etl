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
import scriptella.expressions.ParametersCallback;
import scriptella.jdbc.Query;
import scriptella.spi.QueryCallback;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.Arrays;


/**
 *
 */
public class FilePropertiesTest extends DBTestCase {
    private static final byte FILE[] = "test file/////".getBytes();

    public void test() throws ScriptsExecutorException {
        final Connection con = getConnection("fileproptst");
        AbstractTestCase.testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                return new ByteArrayInputStream(FILE);
            }

            public int getContentLength(final URL u) {
                return FILE.length;
            }
        };

        final ScriptsExecutor se = newScriptsExecutor();
        se.execute();

        Query q = new Query("select (select count(id) from t), c from t");
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
