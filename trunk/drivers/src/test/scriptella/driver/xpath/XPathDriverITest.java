/*
 * Copyright 2006 The Scriptella Project Team.
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
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * Integration test for {@link scriptella.driver.xpath.Driver XPath driver}.
 */
public class XPathDriverITest extends AbstractTestCase {
    private ByteArrayOutputStream o;

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


    public void test() throws EtlExecutorException, UnsupportedEncodingException {
        EtlExecutor e = newEtlExecutor();
        e.execute();
        assertNotNull(o);
        assertEquals("1;2\n3;4\n5;6\n", new String(o.toByteArray()));
    }

}
