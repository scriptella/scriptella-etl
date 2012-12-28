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
package scriptella.driver.ldap.ldif;

import scriptella.AbstractTestCase;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * Tests for {@link TrackingLineIterator}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SubstitutingLineReaderTest extends AbstractTestCase {
    public void test() throws IOException {
        ParametersCallback pc = MockParametersCallbacks.SIMPLE;
        String test = "Just a ${test}\n" +
                "Line2 $var:\n" +
                "\n" +
                "Line${a}4";
        TrackingLineIterator iterator = new TrackingLineIterator(new StringReader(test), pc);
        String s = iterator.next();
        assertEquals("Just a *test*", s);
        assertTrue(iterator.hasNext());
        s = iterator.next();
        assertEquals("Line2 *var*:", s);
        s = iterator.next();
        assertEquals("", s);
        s = iterator.next();
        assertEquals("Line*a*4", s);
        assertFalse(iterator.hasNext()); //EOF
    }

    /**
     * Tests substitution in LDIF files.
     */
    public void testLdif() throws IOException {
        String expected = "dn: cn=Gern Jensen, ou=Product Testing, dc=airius, dc=com\n" +
                "objectclass: top\n" +
                "cn: Gern Jensen\n" +
                "sn: Jensen\n" +
                "uid: gernj\n" +
                "telephonenumber: +1 408 555 1212\n" +
                "description:: V2hhdCBhIGNhcmVmdWwgcmVhZGVyIHlvdSBhcmUhICBUaGlzIHZhbHVl"+
                            "IGlzIGJhc2UtNjQtZW5jb2RlZCBiZWNhdXNlIGl0IGhhcyBhIGNvbnRyb2wgY2hhcmFjdG";
        ParametersCallback pc = new ParametersCallback() {
            public Object getParameter(final String name) {
                if ("cn".equals(name)) {
                    return "Gern Jensen";
                }
                if ("desc".equals(name)) {
                    return "V2hhdCBhIGNhcmVmdWwgcmVhZGVyIHlvdSBhcmUhICBUaGlzIHZhbHVl"+
                            "IGlzIGJhc2UtNjQtZW5jb2RlZCBiZWNhdXNlIGl0IGhhcyBhIGNvbnRyb2wgY2hhcmFjdG";
                }
                return null;
            }
        };

        String str = "dn: cn=${cn}, ou=Product Testing, dc=airius, dc=com\n" +
                "objectclass: top\n" +
                "cn: $cn\n" +
                "sn: Jensen\n" +
                "uid: gernj\n" +
                "telephonenumber: +1 408 555 1212\n" +
                "description:: $desc";
        TrackingLineIterator r = new TrackingLineIterator(new StringReader(str), pc);
        BufferedReader r2 = new BufferedReader(new StringReader(expected));
        for (int i=0;i<7;i++) {
            assertEquals(r2.readLine(), r.next());
        }
        assertFalse(r.hasNext());
        assertNull(r2.readLine());
    }
}
