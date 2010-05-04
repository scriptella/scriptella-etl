/*
 * Copyright 2006-2009 The Scriptella Project Team.
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

/**
 * Integration tests for Bug #6713 and general test of column names and aliases
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ColumnAliasITest extends DBTestCase {
    private boolean executed;
    public void test() throws EtlExecutorException {
        Connection con = getConnection("aliastest");
        newEtlExecutor().execute();
        new QueryHelper("SELECT ID as c1, ID+1, ID+22 as c3 FROM TST").execute(con, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                assertEquals("2", parameters.getParameter("1").toString());
                assertEquals("3", parameters.getParameter("2").toString());
                assertEquals("24", parameters.getParameter("3").toString());
                assertEquals("2", parameters.getParameter("c1").toString());
                assertEquals("24", parameters.getParameter("c3").toString());
                executed=true;
            }
        });
        assertTrue(executed);


    }
}