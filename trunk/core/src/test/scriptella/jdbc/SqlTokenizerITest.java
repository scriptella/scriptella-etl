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
package scriptella.jdbc;

import scriptella.DBTestCase;
import scriptella.execution.EtlExecutorException;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Integration test for {@link SqlTokenizer}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SqlTokenizerITest extends DBTestCase {
    public void test() throws EtlExecutorException {
        Connection con = getConnection("toktest");
        newEtlExecutor().execute();
        //now check the data
        final Set<String> expectedOrcl = new HashSet<String>(Arrays.asList(new String[] {"222", "333", "444"}));
        new QueryHelper("select * from TestOrcl").execute(con, new QueryCallback() {
            private int row=1;
            public void processRow(final ParametersCallback parameters) {
                assertEquals(row, parameters.getParameter("1"));
                Object p = parameters.getParameter("2");
                assertTrue("Unexpected value "+p, expectedOrcl.remove(p));
                row++;
            }
        });
        assertTrue("The following values were not inserted: "+expectedOrcl, expectedOrcl.isEmpty());
        //now test sybase like script, i.e. go separated
        final Set<String> expectedSyb = new HashSet<String>(Arrays.asList(new String[] {"quoted go is ignored\n" +
                "        go\n" +
                "        ", "333", "444"}));
        new QueryHelper("select * from TestSyb").execute(con, new QueryCallback() {
            private int row=1;
            public void processRow(final ParametersCallback parameters) {
                assertEquals(row, parameters.getParameter("1"));
                Object p = parameters.getParameter("2");
                assertTrue("Unexpected value "+p, expectedSyb.remove(p));
                row++;
            }
        });
        assertTrue("The following values were not inserted: "+expectedSyb, expectedSyb.isEmpty());

    }
}
