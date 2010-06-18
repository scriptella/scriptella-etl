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
package scriptella.driver.xpath;

import scriptella.AbstractTestCase;
import scriptella.configuration.ConfigurationFactory;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration test for {@link scriptella.driver.xpath.Driver XPath driver}.
 */
public class XPathDriverITest extends AbstractTestCase {
    private ByteArrayOutputStream o;
    private Map<String, String> params = new HashMap<String, String>();

    protected void setUp() throws Exception {

        o = new ByteArrayOutputStream();
        testURLHandler = new TestURLHandler() {

            public InputStream getInputStream(final URL u) throws IOException {
                throw new UnsupportedOperationException();
            }

            public OutputStream getOutputStream(final URL u) {
                return o;
            }

            public int getContentLength(final URL u) {
                return -1;
            }
        };
    }


    public void test1() throws EtlExecutorException, UnsupportedEncodingException {
        params.put("test", "1");
        EtlExecutor e = newEtlExecutor();
        e.execute();
        assertNotNull(o);
        assertEquals("1;2\n3;4\n5;6\n", new String(o.toByteArray()));
    }
    public void test2() throws EtlExecutorException, UnsupportedEncodingException {
        params.put("test", "2");
        EtlExecutor e = newEtlExecutor();
        e.execute();
        assertNotNull(o);
        assertEquals("1;Column2\n", new String(o.toByteArray()));
    }
    public void test3() throws EtlExecutorException, UnsupportedEncodingException {
        params.put("test", "3");
        EtlExecutor e = newEtlExecutor();
        e.execute();
        assertNotNull(o);
        assertEquals("2\n", new String(o.toByteArray()));
    }
    public void test4() throws EtlExecutorException, UnsupportedEncodingException {
        params.put("test", "4");
        EtlExecutor e = newEtlExecutor();
        e.execute();
        assertNotNull(o);
        assertEquals("1;2\n", new String(o.toByteArray()));
    }

    public void test5() throws EtlExecutorException, UnsupportedEncodingException {
        params.put("test", "5");
        EtlExecutor e = newEtlExecutor();
        e.execute();
        assertNotNull(o);
        assertEquals("2\n", new String(o.toByteArray()));
    }


    @Override
    protected ConfigurationFactory newConfigurationFactory() {
        ConfigurationFactory cf = super.newConfigurationFactory();
        cf.setExternalParameters(params);
        return cf;
    }
}
