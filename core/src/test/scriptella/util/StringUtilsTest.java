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
package scriptella.util;

import scriptella.AbstractTestCase;

import java.util.Random;

/**
 * Tests for{@link StringUtils}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class StringUtilsTest extends AbstractTestCase {
    public void testIsEmpty() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty(" test"));
    }

    public void testIsAsciiWhitespacesOnly() {
        assertTrue(StringUtils.isAsciiWhitespacesOnly(""));
        assertTrue(StringUtils.isAsciiWhitespacesOnly(null));
        assertTrue(StringUtils.isAsciiWhitespacesOnly("      "));
        assertTrue(StringUtils.isAsciiWhitespacesOnly(" \t\r \n "));
        assertTrue(StringUtils.isAsciiWhitespacesOnly("\n"));
        assertFalse(StringUtils.isAsciiWhitespacesOnly(" 1 "));
        assertFalse(StringUtils.isAsciiWhitespacesOnly(" abc "));
        assertFalse(StringUtils.isAsciiWhitespacesOnly("-----"));
    }

    public void testIsDecimalInteger() {
        assertTrue(StringUtils.isDecimalInt("123"));
        assertFalse(StringUtils.isDecimalInt("")); //empty string is not a number
        assertFalse(StringUtils.isDecimalInt(null)); //nulls also
        assertTrue(StringUtils.isDecimalInt("0"));
        assertTrue(StringUtils.isDecimalInt("01"));
        assertFalse(StringUtils.isDecimalInt("01a"));
        assertFalse(StringUtils.isDecimalInt("-1")); //negatives are not supported

        for (int i=0;i<1000;i++) {
            assertTrue("i="+i, StringUtils.isDecimalInt(String.valueOf(i)));
        }
        Random rnd = new Random(0); //use constant seed to simplify reproducing
        for (int i=0;i<1000;i++) {
            int n = rnd.nextInt(Integer.MAX_VALUE);
            assertTrue("n="+n, StringUtils.isDecimalInt(String.valueOf(n)));
        }

    }

    public void testConsoleFormat() {
        String sep = System.getProperty("line.separator");
        String test = "    \u0000 test\r\n  line2\r  line3 ";
        assertEquals("test"+sep+" line2"+sep+" line3", StringUtils.consoleFormat(test));
    }

    public void testRemovePrefix() {
        assertEquals("test", StringUtils.removePrefix("test", null));
        assertEquals(null, StringUtils.removePrefix(null, null));
        assertEquals(null, StringUtils.removePrefix(null,"test"));
        assertEquals("test", StringUtils.removePrefix("url:test","url:"));
    }

    public void testGetMaskedPassword() {
        assertEquals("***", StringUtils.getMaskedPassword("123"));
        assertEquals("", StringUtils.getMaskedPassword(""));
        assertEquals("", StringUtils.getMaskedPassword(null));
        assertEquals("**", StringUtils.getMaskedPassword("**"));
    }

    public void testPad() {
        String result = StringUtils.pad("a", true, 3, ' ');
        assertEquals("  a", result);
        result = StringUtils.pad("b ", false, 3, ' ');
        assertEquals("b  ", result);
        result = StringUtils.pad("c", false, 1, ' ');
        assertEquals("c", result);
        result = StringUtils.pad("c", false, 0, ' ');
        assertEquals("c", result);

        result = StringUtils.pad("d", true, 2, '_');
        assertEquals("_d", result);

        result = StringUtils.pad("", true, 2, '_');
        assertEquals("__", result);

        result = StringUtils.pad(null, true, 2, '_');
        assertEquals("__", result);


    }

}

