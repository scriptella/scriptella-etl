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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Tests tokenizer logic
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class UrlPathTokenizerTest extends AbstractTestCase {
    public void testWin() throws MalformedURLException {
        URL base = new URL("file:/c:/docs/etl.xml");
        String s = "  1.jar;;;:::: lib/second.jar   ;../third.jar:http://5.jar; file:/file name ;    ";
        UrlPathTokenizer tok = new UrlPathTokenizer(base);
        URL[] actual = tok.split(s);
        String[] expected = new String[] {"file:/c:/docs/1.jar", "file:/c:/docs/lib/second.jar",
                "file:/c:/third.jar", "http://5.jar", "file:/file name"};
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i].toString());
        }
    }

    public void testUnix() throws MalformedURLException {
        URL base = new URL("file:/var/etl.xml");
        String s = "1.jar: lib/second.jar :third.jar:;http://5.jar;  ::;  http://ftp:/user";
        UrlPathTokenizer tok = new UrlPathTokenizer(base);
        URL[] actual = tok.split(s);
        String[] expected = new String[] {"file:/var/1.jar", "file:/var/lib/second.jar",
                "file:/var/third.jar", "http://5.jar", "http://ftp:/user"};
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i].toString());
        }
    }

    /**
     * See CR #5029 Automatically convert windows DRIVE:/ paths to file:/ URL
     * Additionally unix absolute paths should be supported.
     */
    public void testAbsolutePathConversion() throws MalformedURLException {
        //Windows path matcher
        //Matching examples: C: C:/ D:/prj/file.etl
        //Not matches: C:// D:test
        UrlPathTokenizer t = new UrlPathTokenizer(new URL("file:/c:/docs/etl.xml"));
        URL[] urls = t.split("d:/;c:/test.txt:e:");
        assertEquals(3, urls.length);
        assertEquals(new URL("file:/d:/"), urls[0]);
        assertEquals(new URL("file:/c:/test.txt"), urls[1]);
        assertEquals(new URL("file:/e:"), urls[2]);

        //Now test absolute unix paths
        urls = t.split("/usr/java;/tmp:test");
        assertEquals(3, urls.length);
        assertEquals(new URL("file:/usr/java"), urls[0]);
        assertEquals(new URL("file:/tmp"), urls[1]);
        assertEquals(new URL("file:/c:/docs/test"), urls[2]);
    }




}
