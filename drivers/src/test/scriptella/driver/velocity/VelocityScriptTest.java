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
package scriptella.driver.velocity;

import scriptella.DBTestCase;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Tests a script with velocity elements.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class VelocityScriptTest extends DBTestCase {
    /**
     * This test runs a script and checks the output produces by the velocity connection.
     * In-memory test URL tst:// is used to avoid file system operations.
     */
    public void test() throws EtlExecutorException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
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
        getConnection("velocity");//just to shutdown at the end of the test
        final EtlExecutor se = newEtlExecutor();
        se.execute();
        final String s = out.toString();
        assertEquals("102030",s);
    }

}
