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
package scriptella.driver.script;

import scriptella.AbstractTestCase;
import scriptella.configuration.ConfigurationFactory;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;

import java.util.HashMap;
import java.util.Map;

/**
 * Integration test for {@link scriptella.driver.script.Driver javax.script Driver}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 * @since 12.01.2007
 */
public class ScriptDriverITest extends AbstractTestCase {
    private Number i;
    private int testNumber;

    /**
     * Test data exchange between JS and scriptella
     *
     * @throws EtlExecutorException
     */
    public void test1() throws EtlExecutorException {
        testNumber = 1;
        EtlExecutor ex = newEtlExecutor();
        ex.execute();
        assertEquals(9, i.intValue());
    }

    /**
     * Test overriding of the variable. For the case
     */
    public void test2() throws EtlExecutorException {
        testNumber = 2;
        EtlExecutor ex = newEtlExecutor();
        ex.execute();
        assertNull(i);
    }


    protected ConfigurationFactory newConfigurationFactory() {
        ConfigurationFactory cf = new ConfigurationFactory();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("callback", this);
        map.put("testNr", testNumber);
        cf.setExternalParameters(map);
        return cf;
    }

    public void notify(Number i) {
        if (testNumber == 1) {
            assertTrue("For test #1 i must be in [0,9] interval", i != null && i.intValue() >= 0 && i.intValue() < 10);
        }
        if (testNumber == 2) {
            assertTrue("For test #2 i must be null", i == null);
        }
        this.i = i;
    }
}
