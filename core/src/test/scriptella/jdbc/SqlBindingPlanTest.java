/*
 * Copyright 2026 The Scriptella Project Team.
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
package scriptella.jdbc;

import scriptella.AbstractTestCase;
import scriptella.spi.ParametersCallback;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SqlBindingPlanTest extends AbstractTestCase {
    public void testOrderDuplicatesAndFreshValues() throws Exception {
        SqlBindingPlan plan = compile("insert into t values (?z, ?a, ?z, ?row.value, ?1)");
        assertNotNull(plan);
        assertEquals("insert into t values (?, ?, ?, ?, ?)", plan.getSql());
        final List<String> names = new ArrayList<String>();
        final Object[] value = {"first"};
        ParametersCallback callback = new ParametersCallback() {
            public Object getParameter(String name) {
                names.add(name);
                return value[0];
            }
        };
        List<Object> values = new ArrayList<Object>();
        plan.bind(callback, values);
        assertEquals(Arrays.asList("z", "a", "z", "row.value", "1"), names);
        assertEquals(Arrays.asList("first", "first", "first", "first", "first"), values);
        values.clear();
        value[0] = null;
        plan.bind(callback, values);
        assertEquals(Arrays.asList(null, null, null, null, null), values);
    }

    public void testQuotesAndComments() throws Exception {
        SqlBindingPlan plan = compile("insert /* ?ignored $ignored */ into t values (?id, '?quoted', \"?quoted\") -- ?ignored\n");
        assertNotNull(plan);
        assertTrue(plan.getSql().contains("values (?, '?quoted', \"?quoted\")"));
        List<Object> values = new ArrayList<Object>();
        plan.bind(new ParametersCallback() {
            public Object getParameter(String name) {
                assertEquals("id", name);
                return 42;
            }
        }, values);
        assertEquals(Arrays.<Object>asList(42), values);
    }

    public void testFallback() throws Exception {
        for (String sql : new String[] {
                "select $column from t where id=?id", "select '?quoted$dynamic' from t where id=?id",
                "select ?{id+1}", "select ?{id==null?1:id}", "select ${column} from t",
                "select ?", "select ?id, ?", "select ?{}", "select 1"}) {
            assertNull(sql, compile(sql));
        }
    }

    private SqlBindingPlan compile(String sql) throws Exception {
        SqlTokenizer tokenizer = new SqlReaderTokenizer(new StringReader(sql));
        try {
            String statement = tokenizer.nextStatement();
            return SqlBindingPlan.compile(statement, tokenizer.getInjections());
        } finally {
            tokenizer.close();
        }
    }
}
