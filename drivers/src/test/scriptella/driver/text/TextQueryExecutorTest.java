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
package scriptella.driver.text;

import scriptella.AbstractTestCase;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.AbstractConnection;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link TextQueryExecutor}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class TextQueryExecutorTest extends AbstractTestCase {
    /**
     * Tests general functionality.
     */
    public void test() {
        StringReader in = new StringReader(
                "---file\n" +
                        "ERROR: msg1 error: msg2\n" +
                        "text\r\n" +
                        "ERROR: msg3");
        TextQueryExecutor tq = new TextQueryExecutor(new StringReader("$severity: (\\w+).*"), new PropertiesSubstitutor(new ParametersCallback() {
            public Object getParameter(final String name) {
                if ("severity".equals(name)) {
                    return "ERROR";
                } else {
                    throw new IllegalArgumentException(name);
                }
            }
        }), true, 0);
        final Set<String> expected = new HashSet<String>();
        expected.add("msg1");
        expected.add("msg3");
        AbstractConnection.StatementCounter cnt = new AbstractConnection.StatementCounter();
        tq.execute(in, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                String p = (String) parameters.getParameter("1");
                assertTrue("Unexpected element " + p, expected.remove(p));
            }
        }, cnt);
        assertEquals(1, cnt.statements);
        assertTrue("The following elements were skipped "+expected, expected.isEmpty());
    }

    public void testEmptyQuery() {
        StringReader in = new StringReader("line1\nline2");
        TextQueryExecutor tq = new TextQueryExecutor(new StringReader(""),
                new PropertiesSubstitutor(MockParametersCallbacks.UNSUPPORTED), false, 0);
        final Set<String> expected = new HashSet<String>();
        expected.add("line1");
        expected.add("line2");
        AbstractConnection.StatementCounter cnt = new AbstractConnection.StatementCounter();
        tq.execute(in, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                String p = (String) parameters.getParameter("0");
                assertTrue("Unexpected element " + p, expected.remove(p));
            }
        }, cnt);
        assertEquals(1, cnt.statements);
        assertTrue("The following elements were skipped "+expected, expected.isEmpty());
    }

    /**
     * Tests if multiline queries are processed correctly.
     */
    public void testQueryMultiline() {
        StringReader in = new StringReader("line1\nline2\nline3\nline4");
        TextQueryExecutor tq = new TextQueryExecutor(new StringReader("line2\nline4"),
                new PropertiesSubstitutor(MockParametersCallbacks.UNSUPPORTED), false, 0);
        final Set<String> expected = new HashSet<String>();
        expected.add("line2");
        expected.add("line4");
        AbstractConnection.StatementCounter cnt = new AbstractConnection.StatementCounter();
        tq.execute(in, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                String p = (String) parameters.getParameter("0");
                assertTrue("Unexpected element " + p, expected.remove(p));
            }
        }, cnt);
        assertEquals(2, cnt.statements);
        assertTrue("The following elements were skipped "+expected, expected.isEmpty());
    }


    /**
     * Tests long content querying.
     */
    public void testLongContent() {
        char[] b = new char[64000];
        Arrays.fill(b, 'a');

        StringReader in = new StringReader(new String(b)+"match111111");
        TextQueryExecutor tq = new TextQueryExecutor(new StringReader(".*(match).*"),
                new PropertiesSubstitutor(MockParametersCallbacks.SIMPLE), true, 0);
        final Set<String> expected = new HashSet<String>();
        expected.add("match");
        AbstractConnection.StatementCounter cnt = new AbstractConnection.StatementCounter();
        tq.execute(in, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                String p = (String) parameters.getParameter("1");
                String p2 = (String) parameters.getParameter("column1");
                assertEquals(p, p2);
                assertTrue("Unexpected element " + p, expected.remove(p));
            }
        }, cnt);
        assertEquals(1, cnt.statements);
        assertTrue("The following elements were skipped "+expected, expected.isEmpty());
    }

    private int lines; //tmp variable to store number of lines read

    /**
     * Tests if skip_lines parameter is working.
     */
    public void testSkipLines() {
        TextQueryExecutor e = new TextQueryExecutor(new StringReader("([^\\t]*)\\t+([^\\t]*)"),
                new PropertiesSubstitutor(MockParametersCallbacks.NULL), false, 2);
        AbstractConnection.StatementCounter cnt = new AbstractConnection.StatementCounter();
        lines=0;
        e.execute(new StringReader("11\t12\n21\t22\n31\t32"), new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                lines++;
                assertEquals("31", parameters.getParameter("1"));
                assertEquals("32", parameters.getParameter("2"));
                assertEquals("31\t32", parameters.getParameter("0"));
            }
        }, cnt);
        assertEquals(1, lines);
        //Now test if number of lines to skip is larger than a file
        cnt.statements=0;
        e.execute(new StringReader(""), new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
            }
        }, cnt);
        assertEquals(1, cnt.statements);

    }
}
