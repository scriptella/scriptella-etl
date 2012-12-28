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
package scriptella.driver.jexl;

import scriptella.AbstractTestCase;
import scriptella.execution.EtlExecutorException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;


/**
 * Integration tests for {@link scriptella.core.EtlVariable}
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlVariableITest extends AbstractTestCase {
    ByteArrayOutputStream out;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        out = new ByteArrayOutputStream();
        testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(URL u) throws IOException {
                return null;
            }

            public OutputStream getOutputStream(URL u) throws IOException {
                return out;
            }

            public int getContentLength(URL u) {
                return 0;
            }
        };
    }

    public void test() throws EtlExecutorException {
        newEtlExecutor().execute();
        String s = out.toString();
        assertEquals("1\n*v2*\nlocal: null444\nglobal: *v2*\n" +
                "i=1\ni=2\n" +
                "if_ok\n", s);
    }


}