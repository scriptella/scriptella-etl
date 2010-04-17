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
package scriptella.driver.jexl;

import scriptella.AbstractTestCase;
import scriptella.configuration.ConfigurationFactory;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;

import java.util.Collections;

/**
 * Integration test for {@link scriptella.driver.jexl.Driver JEXL Driver}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 * @since 12.01.2007
 */
public class JexlDriverITest extends AbstractTestCase {
    private Number i;

    public void test() throws EtlExecutorException {
        EtlExecutor ex = newEtlExecutor();
        ex.execute();
        assertEquals(10, i.intValue());
    }

    protected ConfigurationFactory newConfigurationFactory() {
        ConfigurationFactory cf = new ConfigurationFactory();
        cf.setExternalParameters(Collections.singletonMap("callback", this));
        return cf;
    }

    public void notify(Number i) {
        assertTrue(i != null && i.intValue() > 0 && i.intValue() <= 10);
        this.i = i;
    }
}
