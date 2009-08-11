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
package scriptella.core;

import scriptella.AbstractTestCase;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;

/**
 * Tests for {@link scriptella.execution.EtlExecutor}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlExecutorTest extends AbstractTestCase {
    /**
     * Just a smoke test to make sure run and call methods works correctly.
     * @throws EtlExecutorException if execution fails.
     */
    public void testRunCall() throws EtlExecutorException {
        EtlExecutor exec = newEtlExecutor("EtlExecutorTestRunCall.xml");
        exec.call();
        exec.run();
    }
}
