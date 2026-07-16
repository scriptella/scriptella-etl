/*
 * Copyright 2006-2012 The Scriptella Project Team.
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

import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;
import scriptella.jdbc.QueryHelper;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.sql.Connection;

/**
 * Regression coverage for issue #20: ID result columns must take precedence
 * over a same-named global property in nested scripts.
 */
public class Issue20IdColumnTest extends DBTestCase {
    public void testIdColumnTakesPrecedenceOverGlobalProperty() throws EtlExecutorException {
        final Connection connection = getConnection("issue20idcolumn");
        final EtlExecutor executor = newEtlExecutor("Issue20IdColumnTest.xml");
        executor.execute();

        final int[] rows = new int[1];
        new QueryHelper("select * from Result order by ID").execute(connection,
                new QueryCallback() {
                    public void processRow(final ParametersCallback row) {
                        rows[0]++;
                        Integer id = (Integer) row.getParameter("ID");
                        assertEquals(Integer.valueOf(rows[0]), id);
                        assertEquals(id, row.getParameter("DIRECT_ID"));
                        assertEquals(id, row.getParameter("EXPRESSION_ID"));
                        assertEquals(id, row.getParameter("PARAMETER_ID"));
                        assertEquals(id, row.getParameter("PARAMETER_EXPRESSION_ID"));
                    }
                });
        assertEquals(2, rows[0]);
    }
}
