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
package scriptella.spi;

import scriptella.AbstractTestCase;
import scriptella.configuration.MockConnectionEl;
import scriptella.util.IOUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link ConnectionParameters}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConnectionParametersTest extends AbstractTestCase {
    /**
     * Tests if properties parsing methods work correctly.
     */
    public void testPropertiesParsing() throws ParseException, MalformedURLException, URISyntaxException {
        Map<String, Object> p = new HashMap<String, Object>();
        p.put("int", 10);
        p.put("negative", -10);
        p.put("int2", " 20");
        p.put("boolean", true);
        p.put("boolean2", "yes");
        p.put("url1", "file://test");
        p.put("url2", new URI("file:/url#hash"));
        File f = new File("tmp");
        p.put("url3", f);
        p.put("url4", "file4");
        ConnectionParameters cp = new ConnectionParameters(new MockConnectionEl(p), MockDriverContext.INSTANCE);
        Integer v = cp.getIntegerProperty("nosuchproperty", 1);
        assertEquals(1, v.intValue());
        v = cp.getIntegerProperty("int", 1);
        assertEquals(10, v.intValue());
        v = cp.getIntegerProperty("negative", 1);
        assertEquals(-10, v.intValue());
        v = cp.getIntegerProperty("int2", 1);
        assertEquals(20, v.intValue());
        boolean b = cp.getBooleanProperty("nosuchprop", true);
        assertEquals(true, b);
        b = cp.getBooleanProperty("boolean", false);
        assertEquals(true, b);
        b = cp.getBooleanProperty("boolean2", false);
        assertEquals(true, b);
        //URLs parsing
        assertEquals("file://test", cp.getUrlProperty("url1").toString());
        assertEquals("file:/url#hash", cp.getUrlProperty("url2").toString());
        assertEquals(IOUtils.toUrl(f), cp.getUrlProperty("url3"));
        assertEquals("file:/file4", cp.getUrlProperty("url4").toString());
    }
}
