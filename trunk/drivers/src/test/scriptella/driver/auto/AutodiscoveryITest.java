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
package scriptella.driver.auto;

import scriptella.DBTestCase;
import scriptella.execution.EtlExecutorException;
import scriptella.jdbc.QueryHelper;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

/**
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class AutodiscoveryITest extends DBTestCase {
    int rows;

    public void test() throws EtlExecutorException {
        newEtlExecutor().execute();
        rows = 0;
        new QueryHelper("select * from Autodiscovery").execute(
                getConnection("autotest"), new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                rows++;
                assertEquals(rows, parameters.getParameter("1"));
            }
        });
        assertEquals(2, rows);

    }

}
