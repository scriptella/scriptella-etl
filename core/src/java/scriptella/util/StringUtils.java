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

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Miscellaneous String/CharSequence utility methods.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public final class StringUtils {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final Pattern ANY_CHAR_REGEX = Pattern.compile(".");
    private static final String PASSWORD_MASK = "*";

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
     * Returns a trimmed value for specified charsequence
     *
     * @param cs charsequence to trim.
     * @return trimmed value for specified charsequence or empty string if cs=null
     */
    public static String nullsafeTrim(final CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    /**
     * Returns Null safe string representation of specified object.
     *
     * @param o object to convert to String.
     * @return <code>o.toString()</code> or <code>&quot;&quot;</code> if <code>o==null</code>.
     */
    public static String nullsafeToString(final Object o) {
        return o == null ? "" : o.toString();
    }

    /**
     * Checks if specified characters sequence is empty or contains only ascii whitespace characters.
     *
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
            if (cs.charAt(i) > ' ') {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if specified characters sequence represents a non negative decimal number.
     *
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
            if (c < 0x30 || c > 0x39) {
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
     * <p>String larger than 10KB are trimmed.
     *
     * @param string string to format. Nulls are allowed.
     * @return formatted string.
     */
    public static String consoleFormat(String string) {
        return consoleFormat(string, 10000);
    }

    /**
     * Formats specified string for console suitable representation.
     * <p>All EOL char sequences are replaced with a single system line.separator,
     * and all other whitespace sequences are replaced with a single space.
     *
     * @param string    string to format. Nulls are allowed.
     * @param maxLength maximum number of characters to show or negative if the string cannot be trimmed.
     * @return formatted string.
     */
    public static String consoleFormat(String string, int maxLength) {
        if (string == null) {
            return "";
        }
        String res = string.trim();
        String sep = LINE_SEPARATOR == null ? "\n" : LINE_SEPARATOR;
        res = EOLS.matcher(res).replaceAll(sep);
        res = WHITESPACES.matcher(res).replaceAll(" ");
        if (maxLength > 0 && res.length() > maxLength) {
            res = res.substring(0, maxLength) + " ...";
        }
        return res;
    }

    /**
     * Removes a prefix from a string.
     *
     * @param string original string. May be null.
     * @param prefix prefix to to check and remove. May be null.
     * @return string without a prefix, or unchanged string.
     */
    public static String removePrefix(String string, String prefix) {
        if (prefix != null && string != null && string.startsWith(prefix)) {
            return string.substring(prefix.length());
        }
        return string;
    }

    /**
     * Masks password displaying to user.
     *
     * @param string password.
     * @return masked password or empty string for empty/null password.
     */
    public static String getMaskedPassword(String string) {
        return string == null ? "" : ANY_CHAR_REGEX.matcher(string).replaceAll(PASSWORD_MASK);
    }

    /**
     * Pads a String with a specified character.
     *
     * @param str     string to pad. Null is treated as empty string.
     * @param left    true if left pad, false if right
     * @param width   with of the padded string
     * @param padChar character to use for padding
     * @return padded string.
     */
    public static String pad(String str, boolean left, int width, char padChar) {
        String s = str == null ? "" : str;
        if (width <= s.length()) {
            return s;
        }
        char[] padChars = new char[width - s.length()];
        Arrays.fill(padChars, padChar);
        String padStr = new String(padChars);
        return left ? padStr + s : s + padStr;
    }

}
