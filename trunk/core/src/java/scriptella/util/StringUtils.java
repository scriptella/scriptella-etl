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
package scriptella.util;

import java.util.regex.Pattern;

/**
 * Miscellaneous String/CharSequence utility methods.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public final class StringUtils {
    private StringUtils() {//singleton
    }

    /**
     * Returns true if characters sequence is empty (length=0) or null.
     *
     * @param cs characters sequence to test.
     * @return true if characters sequence is empty (length=0) or null.
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * Checks if specified characters sequence is empty or contains only ascii whitespace characters.
     * @param cs characters sequence to check.
     * @return true if characters sequence is empty or contains only ascii whitespace characters.
     */
    public static boolean isAsciiWhitespacesOnly(final CharSequence cs) {
        if (cs == null) {
            return true;
        }
        int len = cs.length();
        if (len == 0) {
            return true;
        }
        for (int i = 0; i < len; i++) {
            if (cs.charAt(i)>' ') {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if specified characters sequence represents a non negative decimal number.
     * @param cs characters sequence to check.
     * @return true if specified characters sequence represents a non negative decimal number.
     */
    public static boolean isDecimalInt(final CharSequence cs) {
            if (cs == null) { //null is not a number.
                return false;
            }
            int len = cs.length();
            if (len == 0) { //empty string also
                return false;
            }
            for (int i = 0; i < len; i++) {
                int c = cs.charAt(i);
                if (c<0x30 || c>0x39) {
                    return false;
                }
            }
            return true;
        }

    private static Pattern WHITESPACES = Pattern.compile("[\\x00-\\x20&&[^\\r\\n]]+");
    private static Pattern EOLS = Pattern.compile("[\\r\\n]+");

    /**
     * Formats specified string for console suitable representation.
     * <p>All EOL char sequences are replaced with a single system line.separator,
     * and all other whitespace sequences are replaced with a single space.
     * @param string string to format. Nulls are allowed.
     * @return formatted string.
     */
    public static String consoleFormat(String string) {
        if (string==null) {
            return "";
        }
        String res = string.trim();
        String sep = System.getProperty("line.separator");
        if (sep==null) {
            sep="\n";
        }
        res = EOLS.matcher(res).replaceAll(sep);
        res = WHITESPACES.matcher(res).replaceAll(" ");
        return res;
    }



}
