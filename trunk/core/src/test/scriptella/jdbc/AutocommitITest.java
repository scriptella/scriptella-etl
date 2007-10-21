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
 * Integration tests for {@link scriptella.jdbc.JdbcConnection#autocommit} and
 * {@link scriptella.jdbc.JdbcConnection#autocommitSize} parameters.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class AutocommitITest extends DBTestCase {
    public void test() throws EtlExecutorException {
        Connection con = getConnection("autocommititest");
        newEtlExecutor().execute();
        final Set<String> ids = new HashSet<String>();
        new QueryHelper("select * from tst").execute(con, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                ids.add(String.valueOf(parameters.getParameter("1")));
            }
        });
        Set<String> expected = new HashSet<String>(Arrays.asList("1", "2", "3", "4", "5"));
        assertEquals(expected, ids);


    }
}
