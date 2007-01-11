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
package scriptella.execution;

import scriptella.AbstractTestCase;
import scriptella.configuration.ConfigurationFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for lazy initialized connections.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class LazyInitConnectionTest extends AbstractTestCase {
    private Map<String,String> props;

    protected ConfigurationFactory newConfigurationFactory() {
        ConfigurationFactory cf = super.newConfigurationFactory();
        cf.setExternalProperties(props);
        return cf;
    }

    public void test() throws EtlExecutorException {
        //script should execute normally because of lazy init
        props=new HashMap<String, String>();
        props.put("lazy", "true");
        EtlExecutor exec = newEtlExecutor();
        exec.execute();
        //now ETL fails because of greedy mode
        props.put("lazy", "false");
        exec = newEtlExecutor();
        try {
            exec.execute();
            fail("Script should fail in a greedy init mode.");
        } catch (EtlExecutorException e) {
            //OK
        }

    }
}
