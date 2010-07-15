/*
 * Copyright 2006-2010 The Scriptella Project Team.
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

import java.io.*;
import java.net.URL;

/**
 * Integration test for text query
 *
 * @author Kirill Volgin
 * @version 1.0
 */
public class TextQueryITest extends AbstractTestCase {

    private ByteArrayOutputStream out;

    protected void setUp() throws Exception {

        testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                if (u.toString().endsWith("2")) {
                    throw new UnsupportedOperationException();
                }
                String in =
                        "110123456789\n" +
                        "2201234     \n" +
                        "330123456789\n" +
                        "44012345678 \n" +
                        "5501234567890\n" +
                        "66\n" +
                        "77          ";
                return new ByteArrayInputStream(in.getBytes());
            }

            public OutputStream getOutputStream(final URL u) {
                if (!u.toString().endsWith("2")) {
                    throw new UnsupportedOperationException();
                }
                return out = new ByteArrayOutputStream();
            }

            public int getContentLength(final URL u) {
                return -1;
            }
        };
    }


    public void test() throws EtlExecutorException, UnsupportedEncodingException {
        EtlExecutor e = newEtlExecutor();
        e.execute();
        assertNotNull(out);
        assertEquals(
                "11, 0123456789\n" +
                "22, 01234     \n" +
                "33, 0123456789\n" +
                "44, 012345678 \n" +
                "55, 0123456789\n" +
                "77,           \n", new String(out.toByteArray()));

    }

}