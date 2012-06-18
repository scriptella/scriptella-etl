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
package scriptella.text;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds metadata about column format.
 * <p>Used for storing formatting/parsing rules of CSV and other text files.</p>
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class ColumnFormatInfo {
    public static final String NULL_STRING = "null_string";
    private Map<String, ColumnFormat> formatMap;
    private String nullString;

    public ColumnFormatInfo(Map<String, ColumnFormat> formatMap, String nullString) {
        this.formatMap = new HashMap<String, ColumnFormat>(formatMap);
        this.nullString = nullString;
    }

    public ColumnFormat getColumnInfo(String name) {
        return formatMap.get(name);
    }

    Map<String, ColumnFormat> getFormatMap() {
        return formatMap;
    }

    public boolean isEmpty() {
        return formatMap.isEmpty();
    }

    public String getNullString() {
        return nullString;
    }

    /**
     * Creates a {@link ColumnFormatInfo} from specified properties.
     *
     * @param properties properties defining column formatting.
     * @param prefix     prefix for recognizing formatting properties.
     * @return {@link ColumnFormatInfo} with a specified column formatting.
     */
    public static ColumnFormatInfo parse(TypedPropertiesSource properties, String prefix) {
        Map<String, ColumnFormat> map = new LinkedHashMap<String, ColumnFormat>();
        for (String key: properties.getKeys()) {
            if (isPrefixed(key, prefix)) {
                String cleanKey = removePrefix(key, prefix);
                //property name determined by the last component
                int dotPos = cleanKey.lastIndexOf('.');
                if (dotPos > 0) {
                    String columnName = cleanKey.substring(0, dotPos);
                    String columnProp = cleanKey.substring(dotPos + 1);
                    ColumnFormat ci = map.get(columnName);
                    if (ci == null) {
                        ci = new ColumnFormat();
                        map.put(columnName, ci);
                    }
                    setProperty(ci, columnProp, key, properties);
                }
            }
        }
        String nullString = properties.getStringProperty(prefix == null ? NULL_STRING : prefix + NULL_STRING);
        return new ColumnFormatInfo(map, nullString);
    }

    protected static void setProperty(ColumnFormat f, String columnPropName, String key, TypedPropertiesSource ps) {
        if ("pattern".equalsIgnoreCase(columnPropName)) {
            f.setPattern(ps.getStringProperty(key));
        } else if ("null_string".equalsIgnoreCase(columnPropName)) {
            f.setNullString(ps.getStringProperty(key));
        } else if ("locale".equalsIgnoreCase(columnPropName)) {
            f.setLocale(ps.getLocaleProperty(key));
        } else if ("trim".equalsIgnoreCase(columnPropName)) {
            f.setTrim(ps.getBooleanProperty(key, false));
        } else if ("type".equalsIgnoreCase(columnPropName)) {
            f.setType(ps.getStringProperty(key));
        }
    }

    private static String removePrefix(String key, String prefix) {
        if (prefix == null) {
            return key;
        }
        return key.substring(prefix.length());
    }

    private static boolean isPrefixed(String key, String prefix) {
        return prefix == null || key.startsWith(prefix);
    }

}
