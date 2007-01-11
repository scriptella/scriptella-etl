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

import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Collections utility methods.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public final class CollectionUtils {
    private CollectionUtils() {//Singleton
    }

    /**
     * Comparator similar to {@link String#CASE_INSENSITIVE_ORDER}, but
     * handles only ASCII characters
     */
    private static final Comparator<String> ASCII_CASE_INSENSITIVE_ORDER = new Comparator<String>() {
        public int compare(String s1, String s2) {
            int n1=s1.length(), n2=s2.length();
            int n=n1<n2?n1:n2;
            for (int i=0; i<n; i++) {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(i);
                if (c1 != c2) {
                    if (c1>='A' && c1<='Z') { //Fast lower case
                        c1=(char)(c1|0x20);
                    }
                    if (c2>='A' && c2<='Z') {
                        c2=(char)(c2|0x20);
                    }
                    if (c1 != c2) {
                        return c1 - c2;
                    }
                }
            }
            return n1 - n2;
        }
    };

    /**
     * Create a map optimized for case insensitive search for keys.
     * The case insensitive rules are simplified to ASCII chars for performance reasons.
     *
     * @return case insensitive map.
     */
    public static <V> Map<String, V> newCaseInsensitiveAsciiMap() {
        return new TreeMap<String, V>(ASCII_CASE_INSENSITIVE_ORDER);
    }

    /**
     * Returns parameterized version of {@link Properties} the instance
     * remains the same.
     * @param properties properties to represent as a map.
     */
    @SuppressWarnings("unchecked")
    public static Map<String,String> asMap(Properties properties) {
        return (Map)properties;
    }
}
