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
import scriptella.jdbc.Query;
import scriptella.spi.QueryCallback;

import java.sql.Connection;
import java.sql.Timestamp;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JDBCEscapingTest extends DBTestCase {
    /**
     * Tests correct execution of scripts with jdbc escaped timestamp data.
     *
     * @throws ScriptsExecutorException
     */
    public void testTimestamps() throws ScriptsExecutorException {
        final Connection c = getConnection("jdbcet");
        final ScriptsExecutor se = newScriptsExecutor();
        se.execute();

        Query q = new Query("select * from test");
        final Timestamp expectedTs = Timestamp.valueOf("2005-10-10 22:33:44.1");
        q.execute(c,
                new QueryCallback() {
                    public void processRow(final ParametersCallback rowEvaluator) {
                        final Timestamp ts = (Timestamp) rowEvaluator.getParameter(
                                "d");
                        assertEquals(expectedTs, ts);
                    }
                });
    }
}
