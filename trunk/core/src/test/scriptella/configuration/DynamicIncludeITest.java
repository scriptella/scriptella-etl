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
package scriptella.configuration;

import scriptella.DBTestCase;
import scriptella.execution.EtlExecutorException;
import scriptella.jdbc.QueryHelper;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Integration tests for dynamic include functionality, e.g.<code> &lt;include href="$path"/></code>
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DynamicIncludeITest extends DBTestCase {
    public void test() throws EtlExecutorException {
        newEtlExecutor().execute();
        QueryHelper qh = new QueryHelper("SELECT * FROM Data");
        final Set<Integer> expected = new HashSet<Integer>(Arrays.asList(1, 2, -1));

        qh.execute(getConnection("dynamicInclude"), new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                Integer p = (Integer) parameters.getParameter("1");
                assertTrue("Unexpected number: " + p + ", expected values are: " + expected, expected.remove(p));
            }
        });
        assertTrue("The following nums were not inserted: " + expected, expected.isEmpty());


    }
}
