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

package scriptella.driver.script;

import scriptella.AbstractTestCase;
import scriptella.TestLoggingConfigurer;
import scriptella.configuration.ConfigurationFactory;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Integration test of JavaScript queries.
 *
 * @author Fyodor Kupolov
 * @see scriptella.driver.jexl.JexlQueryITest
 */
public class ScriptingQueryITest extends AbstractTestCase {
    private List<Integer> results = new ArrayList<Integer>();
    private TestLoggingConfigurer loggingConfigurer = new TestLoggingConfigurer(MissingQueryNextCallDetector.class.getName());

    @Override
    protected void setUp() throws Exception {
        loggingConfigurer.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        loggingConfigurer.tearDown();
    }

    public void test() throws EtlExecutorException {
        EtlExecutor ex = newEtlExecutor();
        ex.execute();
        assertEquals(Arrays.asList(2, 3), results);
        //For case 1 and 4, expect warnings about missing query.next
        assertEquals(1, loggingConfigurer.getMessageCount("query.next() was never called in query /etl/query[1]"));
        assertEquals(1, loggingConfigurer.getMessageCount("query.next() was never called in query /etl/query[1]"));
        assertEquals(2, loggingConfigurer.getMessageCount("query.next() was never called in query"));
    }

    protected ConfigurationFactory newConfigurationFactory() {
        ConfigurationFactory cf = new ConfigurationFactory();
        cf.setExternalParameters(Collections.singletonMap("callback", this));
        return cf;
    }

    public void notify(Number i) {
        results.add(i.intValue());
    }

}
