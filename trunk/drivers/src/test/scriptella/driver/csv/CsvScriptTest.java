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
package scriptella.driver.csv;

import scriptella.DBTestCase;
import scriptella.execution.ScriptsExecutor;
import scriptella.execution.ScriptsExecutorException;
import scriptella.jdbc.QueryHelper;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests CSV script processing.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class CsvScriptTest extends DBTestCase {
    public void test() throws ScriptsExecutorException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                return new ByteArrayInputStream("f1,f2,f3\n1,2,3\n4,5,6\n7,8,9".getBytes());
            }

            public OutputStream getOutputStream(final URL u) {
                return out;
            }

            public int getContentLength(final URL u) {
                throw new UnsupportedOperationException();
            }
        };
        final ScriptsExecutor se = newScriptsExecutor();
        se.execute();
        final Connection connection = getConnection("csv");
        QueryHelper q = new QueryHelper("SELECT * from Result");
        final Set<String> expected = new HashSet<String>();
        expected.add("1 2 3");expected.add("4 5 6");expected.add("7 8 9");
        expected.add("q1"); expected.add("q4");//second query filter only first and second rows
        q.execute(connection, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                final Object parameter = parameters.getParameter("text");
                assertTrue("Row "+parameter+" is not expected", expected.remove(parameter));
            }
        });
        final String s = out.toString();
        assertEquals("\"1\",\"One\"\n" +
                "\"2\",\" ;,-Two\"\" \"\n" +
                "\"3\",\" Three!!,  \"\n",s);

    }
}
