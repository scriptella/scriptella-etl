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
import scriptella.jdbc.QueryHelper;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DialectsTest extends DBTestCase {
    public void test() throws ScriptsExecutorException {
        final Connection con = getConnection("dialectstest");
        final ScriptsExecutor se = newScriptsExecutor("DialectsTest.xml");
        se.execute();

        QueryHelper s = new QueryHelper("select * from test");
        final Set expected = new HashSet(Arrays.asList(
                new Integer[]{1, 3, 4, 5, 6, 7, 9}));
        final Set actual = new HashSet<Integer>();

        s.execute(con,
                new QueryCallback() {
                    public void processRow(final ParametersCallback row) {
                        actual.add(row.getParameter("ID"));
                    }
                });
        assertEquals(expected, actual);
    }

    public void test2() throws ScriptsExecutorException {
        final Connection con = getConnection("dialectstest2");
        final ScriptsExecutor se = newScriptsExecutor("DialectsTest2.xml");
        se.execute();

        QueryHelper s = new QueryHelper("select * from test2");
        final Set expected = new HashSet(Arrays.asList(new Integer[]{1, 2}));
        final Set actual = new HashSet<Integer>();

        s.execute(con,
                new QueryCallback() {
                    public void processRow(final ParametersCallback row) {
                        actual.add(row.getParameter("ID"));
                    }
                });
        assertEquals(expected, actual);
    }
}
