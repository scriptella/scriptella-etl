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
package scriptella.driver.velocity;

import scriptella.DBTestCase;
import scriptella.configuration.ConfigurationFactory;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests a script with velocity elements.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class VelocityScriptTest extends DBTestCase {
    private Map<String, Object> params = new HashMap<String, Object>();
    private ByteArrayOutputStream out = new ByteArrayOutputStream();


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                throw new UnsupportedOperationException();
            }

            public OutputStream getOutputStream(final URL u) {
                return out;
            }

            public int getContentLength(final URL u) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    protected void tearDown() throws Exception {
        testURLHandler = null;
        out = null;
        super.tearDown();
    }

    /**
     * This test runs a script and checks the output produces by the velocity connection.
     * In-memory test URL tst:// is used to avoid file system operations.
     * @throws scriptella.execution.EtlExecutorException if error occurs
     */
    public void test() throws EtlExecutorException {
        params.put("test", 1);
        getConnection("velocity");//just to shutdown at the end of the test
        final EtlExecutor se = newEtlExecutor();
        se.execute();
        final String s = out.toString();
        assertEquals("102030", s);
    }

    /**
     * History test for [BUG-12284]  -  Velocity driver doesn't support foreach
     * @throws EtlExecutorException if error occurs
     */
    public void test2() throws EtlExecutorException {
        params.put("test", 2);
        getConnection("velocity");//just to shutdown at the end of the test
        final EtlExecutor se = newEtlExecutor();
        se.execute();
        final String s = out.toString();
        assertEquals("\n" +
                "            2\n" +
                "            3\n" +
                "        ", s);
    }

    @Override
    protected ConfigurationFactory newConfigurationFactory() {
        ConfigurationFactory cf = super.newConfigurationFactory();
        cf.setExternalParameters(params);
        return cf;
    }
}
