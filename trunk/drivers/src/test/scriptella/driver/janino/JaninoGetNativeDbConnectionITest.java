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
package scriptella.driver.janino;

import scriptella.AbstractTestCase;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;

/**
 * Integration test for {@link scriptella.driver.jexl.Driver JEXL Driver}.
 *
 * @author Fyodor Kupolov
 * @version 1.2
 */
public class JaninoGetNativeDbConnectionITest extends AbstractTestCase {

    private static int count; // set from ETL script by calling JaninoGetNativeDbConnectionITest.runStatic

    public void test() throws EtlExecutorException {
        EtlExecutor ex = newEtlExecutor();
        ex.execute();
        assertEquals("The ETL script is expected to call runStatic(1)", 1, count);
    }

    // Called from test ETL file
    public static void runStatic(Object result) {
        count = ((Number)result).intValue();
    }
}
