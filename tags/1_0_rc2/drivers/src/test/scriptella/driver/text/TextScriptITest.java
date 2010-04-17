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
package scriptella.driver.text;

import scriptella.AbstractTestCase;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * Integration test for text script
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class TextScriptITest extends AbstractTestCase {
    private ByteArrayOutputStream o1;
    private ByteArrayOutputStream o2;

    protected void setUp() throws Exception {

        testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                if (u.toString().endsWith("2")) {
                    throw new UnsupportedOperationException();
                }
                return new ByteArrayInputStream("a1\nb22b\n\rc333c".getBytes());
            }

            public OutputStream getOutputStream(final URL u) {
                if (u.toString().endsWith("2")) {
                    return o2 = new ByteArrayOutputStream();
                } else {
                    return o1 = new ByteArrayOutputStream();
                }
            }

            public int getContentLength(final URL u) {
                return -1;
            }
        };
    }


    public void test() throws EtlExecutorException, UnsupportedEncodingException {
        EtlExecutor e = newEtlExecutor();
        e.execute();
        assertNotNull(o1);
        assertEquals("Test Trim\n", new String(o1.toByteArray()));
        assertNotNull(o2);
        assertEquals("n1\rn22\rn333\r", new String(o2.toByteArray()));

    }

}
