/*
 * Copyright 2006-2020 The Scriptella Project Team.
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
package scriptella.driver.shell;

import scriptella.AbstractTestCase;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;
import scriptella.spi.MockConnectionParameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Integration test for shell script/queries
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ShellDriverITest extends AbstractTestCase {
    private ByteArrayOutputStream o1;
    private ByteArrayOutputStream o2;

    protected void setUp() throws Exception {

        testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                throw new UnsupportedOperationException();
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
        assertNotNull(o2);
        String outputStr = new String(o1.toByteArray());
        // Normally unit tests should not rely on the running environment
        // but this is an integration test, so we actually verify that behavior is OS specific
        assertTrue("Unexpected output: '" + outputStr + "'", outputStr.contains("Running"));

        // Check that only one Running word appears
        int running = outputStr.replace("Running", "").length();
        assertEquals("Unexpected format - " + outputStr, "Running".length(),outputStr.length() - running);

        ShellConnectionParameters p = new ShellConnectionParameters(new MockConnectionParameters());
        String expectedOsName = p.getOsBehavior().name().toLowerCase();
        assertTrue("Expected " + expectedOsName + ", but found " + outputStr, outputStr.toLowerCase().contains(expectedOsName));

        // Now verify the query
        outputStr = new String(o2.toByteArray());
        Pattern expected = Pattern.compile("\\s*Exported Line1\\s*Exported Line2\\s*Done\\s*");
        assertTrue("Unexpected result: " + outputStr, expected.matcher(outputStr).matches());
    }


}
