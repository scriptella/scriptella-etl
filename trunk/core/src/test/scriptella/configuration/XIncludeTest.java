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
package scriptella.configuration;

import scriptella.AbstractTestCase;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class XIncludeTest extends AbstractTestCase {
    public void testClasspathResource() {
        URL url = getClass().getResource("XIncludeTest.xml");
        test(url);
    }

    public void testFileResource() throws MalformedURLException {
        URL u = getFileResource(getClass().getPackage().getName().replace('.','/')+"/XIncludeTest.xml").toURL();
        test(u);
    }

    private void test(final URL url) {
        ConfigurationFactory cf = new ConfigurationFactory();
        cf.setResourceURL(url);

        final ConfigurationEl c = cf.createConfiguration();
        final List<ScriptingElement> scripts = c.getScriptingElements();
        assertEquals(scripts.size(), 4);

        String text = asString(scripts.get(0).getDialectContent(null));
        String str = "insert into test(id, value) values (2,'333');";
        assertTrue("Script \n" + removeWhitespaceChars(text) +
                "\n must contain substring: " + str, text.indexOf(str) > 0);
        text = asString(scripts.get(1).getDialectContent(null));
        str = "insert into test2(id, value) values (3,'444');";
        assertTrue("Script \n" + removeWhitespaceChars(text) +
                "\n must contain substring: " + str, text.indexOf(str) > 0);
        text = asString(scripts.get(2).getDialectContent(null));
        str = "insert into test(id, value) values (2,'333');";
        assertTrue("Script \n" + removeWhitespaceChars(text) +
                "\n must contain substring: " + str, text.indexOf(str) > 0);
        str = "--Sample1--";
        assertTrue("Script \n" + removeWhitespaceChars(text) +
                "\n must contain substring: " + str, text.indexOf(str) > 0);
        str = "--Sample2--";
        assertTrue("Script \n" + removeWhitespaceChars(text) +
                "\n must contain substring: " + str, text.indexOf(str) > 0);
        //Fallback test
        text = asString(scripts.get(3).getDialectContent(null));
        str = "Fallback!";
        assertTrue("Script \n" + removeWhitespaceChars(text) +
                "\n must contain substring: " + str, text.indexOf(str) > 0);

    }

    private String asString(final ContentEl content) {
        char cb[] = new char[4096];
        StringBuilder sb = new StringBuilder(8192);

        try {
            Reader reader = content.open();

            for (int c = 0; (c = reader.read(cb)) > 0;) {
                sb.append(cb, 0, c);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return sb.toString();
    }
}
