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
package scriptella.driver.text;

import scriptella.AbstractTestCase;
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
        TextQueryExecutor tq = new TextQueryExecutor("error: (\\w+).*", in, MockParametersCallbacks.UNSUPPORTED);
        final Set<String> expected = new HashSet<String>();
        expected.add("msg1");
        expected.add("msg3");
        tq.execute(new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                String p = (String) parameters.getParameter("1");
                assertTrue("Unexpected element " + p, expected.remove(p));
            }
        });
        assertTrue("The following elements were skipped "+expected, expected.isEmpty());
    }

    /**
     * Tests long content querying.
     */
    public void testLongContent() {
        char[] b = new char[64000];
        Arrays.fill(b, 'a');

        StringReader in = new StringReader(new String(b)+"match111111");
        TextQueryExecutor tq = new TextQueryExecutor(".*(match).*", in, MockParametersCallbacks.SIMPLE);
        final Set<String> expected = new HashSet<String>();
        expected.add("match");
        tq.execute(new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                String p = (String) parameters.getParameter("1");
                assertTrue("Unexpected element " + p, expected.remove(p));
            }
        });
        assertTrue("The following elements were skipped "+expected, expected.isEmpty());

    }
}
