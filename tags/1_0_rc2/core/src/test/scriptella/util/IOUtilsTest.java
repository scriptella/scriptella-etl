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
package scriptella.util;

import scriptella.AbstractTestCase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * Tests for {@link scriptella.util.IOUtils}.
 */
public class IOUtilsTest extends AbstractTestCase {
    public void testToUrl() throws MalformedURLException {
        URL url = IOUtils.toUrl(new File("tst 2"));
        assertTrue(url.toString().startsWith("file:/"));
        assertTrue(url.toString().endsWith("tst%202"));
    }

    public void testToByteArray() throws IOException {
        byte[] expected = new byte[]{1, 2, 3, 4};
        assertTrue(Arrays.equals(expected, IOUtils.toByteArray(new ByteArrayInputStream(expected))));
    }

    public void testToCharArray() throws IOException {
        String expected = "test1234\u0000";
        assertEquals(expected, IOUtils.toString(new StringReader(expected)));
    }

    public void testResolve() throws MalformedURLException {
        URL base = new URL("file:/c:/docs/etl.xml");
        assertEquals(new URL("file:/d:"), IOUtils.resolve(base, "d:"));
        assertEquals(new URL("file:/d:/test.txt"), IOUtils.resolve(base, "d:/test.txt"));
        try {
            String malformed = "d://test.txt";
            IOUtils.resolve(base, malformed);
            fail("Malformed url " + malformed + " must be rejected");
        } catch (MalformedURLException e) {
            //OK
        }
    }

}
