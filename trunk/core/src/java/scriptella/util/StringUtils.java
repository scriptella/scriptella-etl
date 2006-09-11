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

/**
 * Miscellaneous String utility methods.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public final class StringUtils {
    private StringUtils() {//singleton
    }

    /**
     * Returns true if string is empty (length=0) or null.
     *
     * @param string string to test.
     * @return true if string is empty (length=0) or null.
     */
    public static boolean isEmpty(final String string) {
        return string == null || string.length() > 0;
    }

    /**
     * Checks if specified string is empty or contains only ascii whitespace characters.
     * @param string string to check.
     * @return true if string is empty or contains only ascii whitespace characters.
     */
    public static boolean isAsciiWhitespacesOnly(final String string) {
        if (string == null) {
            return true;
        }
        int len = string.length();
        if (len == 0) {
            return true;
        }
        for (int i = 0; i < len; i++) {
            switch (string.charAt(i)) {
                case 0x09://tab
                case 0x0A:
                case 0x0B:
                case 0x0C:
                case 0x0D:
                case 0x1C:
                case 0x1D:
                case 0x1E:
                case 0x1F:
                case 0x20: break;
                default: return false;
            }
        }
        return true;
    }

    /**
     * Checks if specified string represents a decimal number.
     * @param string string to check.
     * @return true if specified string represents a decimal number.
     */
    public static boolean isDecimalNumber(final String string) {
            if (string == null) { //null is not a number.
                return false;
            }
            int len = string.length();
            if (len == 0) { //empty string also
                return false;
            }
            for (int i = 0; i < len; i++) {
                int c = string.charAt(i);
                if (c<0x30 || c>0x39) {
                    return false;
                }
            }
            return true;
        }


}
