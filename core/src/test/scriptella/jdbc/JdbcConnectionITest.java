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

import scriptella.AbstractTestCase;
import scriptella.execution.EtlExecutorException;

/**
 * Integration test for JDBC connection.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JdbcConnectionITest extends AbstractTestCase {
    public void test() throws EtlExecutorException {
        //For now just a smoke test
        newEtlExecutor().execute();
    }
}
