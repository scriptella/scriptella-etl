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
package scriptella.driver.janino;

import scriptella.AbstractTestCase;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for Janino Query/Script base classes.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JaninoBaseClassesTest extends AbstractTestCase {
    /**
     * Tests public API methods provided by {@link JaninoScript}.
     */
    public void testScript() {
        JaninoScript js = new JaninoScript() {
            protected void execute() throws Exception {
            }
        };
        js.setParametersCallback(MockParametersCallbacks.SIMPLE);
        assertEquals("*1*", js.get("1"));
    }
    /**
     * Tests public API methods provided by {@link JaninoQuery}.
     */
    public void testQuery() {
        final int[] rows = new int[1]; //just to allow inner classes to modify a variable
        JaninoQuery jq = new JaninoQuery() {
            protected void execute() throws Exception {
            }
        };
        //now check a query exposing a row with one column
        jq.setParametersCallback(new ParametersCallback() {
            public Object getParameter(final String name) {
                if (!"2".equals(name)) {
                    fail("Only parameter with name 2 was not specified, but query requested "+name);
                }
                return null;
            }
        });
        jq.setQueryCallback(new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                assertEquals("v1", parameters.getParameter("1"));
                assertEquals(null, parameters.getParameter("2"));
                rows[0]++;
            }
        });
        jq.set("1", "v1");
        jq.next();//1st row
        //now multiple columns in a row
        //Query provide enough columns - no need to get parameters from parent
        jq.setParametersCallback(MockParametersCallbacks.UNSUPPORTED);

        jq.setQueryCallback(new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                assertEquals("v1", parameters.getParameter("1"));
                assertEquals("v2", parameters.getParameter("2"));
                rows[0]++;
            }
        });
        jq.set("1", "v1");
        jq.set("2", "v2");
        jq.next();//2nd row
        jq.next(new String[] {"1", "2"},new Object[] {"v1", "v2"}); //3rd row
        //Now test passing a map
        Map<String,Object> m = new HashMap<String, Object>();
        m.put("1", "v1");
        m.put("2", "v2");
        jq.set(m);
        jq.next(); //4th row
        jq.next(m); //5th row

        assertEquals(5, rows[0]);
    }

}
