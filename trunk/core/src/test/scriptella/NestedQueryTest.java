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
import scriptella.sql.Query;
import scriptella.sql.QueryCallback;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * This class provides several tests for checking transaction attributes processing.
 */
public class NestedQueryTest extends DBTestCase {
    public void test() throws ScriptsExecutorException {
        final Connection con = getConnection("nestedquerytestdb2");

        final ScriptsExecutor se = newScriptsExecutor();
        se.execute();

        Query s = new Query("select * from result");
        final Set<Integer> ids = new HashSet<Integer>();
        Set<Integer> expectedIds = new HashSet<Integer>(Arrays.asList(
                new Integer[]{1, 3}));
        final Set<String> texts = new HashSet<String>();
        Set<String> expectedTexts = new HashSet<String>(Arrays.asList(
                new String[]{"One", "Three"}));

        s.execute(con,
                new QueryCallback() {
                    public void processRow(final ParametersCallback evaluator) {
                        final Integer id = (Integer) evaluator.getParameter("id");
                        ids.add(id);

                        final String text = (String) evaluator.getParameter("text");
                        texts.add(text);
                        assertEquals("!tst", evaluator.getParameter("text2"));
                    }
                });
        assertEquals(ids, expectedIds);
        assertEquals(texts, expectedTexts);
    }

    public void test2() throws ScriptsExecutorException {
        final Connection con = getConnection("nestedquerytest2db2");

        final ScriptsExecutor se = newScriptsExecutor("NestedQueryTest2.xml");
        se.execute();

        Query s = new Query("select * from test");
        final Set<Integer> ids = new HashSet<Integer>();
        Set<Integer> expectedIds = new HashSet<Integer>(Arrays.asList(
                new Integer[]{5, 7, 13, 15, 2, 6}));

        s.execute(con,
                new QueryCallback() {
                    public void processRow(final ParametersCallback evaluator) {
                        final Integer id = (Integer) evaluator.getParameter("id");
                        ids.add(id);
                    }
                });
        assertEquals(ids, expectedIds);
    }

    /**
     * Tests rownum pseudo-column.
     */
    public void testRownum() throws ScriptsExecutorException {
        final Connection con = getConnection("nestedquerytestrownum");

        final ScriptsExecutor se = newScriptsExecutor("NestedQueryTestRownum.xml");
        se.execute();

        Query s = new Query("select * from Result");
        final Set<Integer> ids = new HashSet<Integer>();
        Set<Integer> expectedIds = new HashSet<Integer>(Arrays.asList(
                new Integer[]{1, 2, 3, 11, 12, 13}));

        s.execute(con,
                new QueryCallback() {
                    public void processRow(final ParametersCallback evaluator) {
                        final Integer id = (Integer) evaluator.getParameter("id");
                        ids.add(id);
                    }
                });
        assertEquals(expectedIds, ids);
    }

}
