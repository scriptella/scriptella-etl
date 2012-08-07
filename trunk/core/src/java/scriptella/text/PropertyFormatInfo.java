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

import scriptella.spi.ConnectionParameters;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds metadata about properties format.
 * <p>Used for storing formatting/parsing rules of CSV and other text files.</p>
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class PropertyFormatInfo {
    public static final String NULL_STRING = "null_string";
    public static final String TRIM = "trim";
    public static final String CLASS_NAME = "className";
    public static final String PATTERN = "pattern";
    public static final String LOCALE = "locale";
    public static final String TYPE = "type";
    public static final String PAD_LEFT = "pad_left";
    public static final String PAD_RIGHT = "pad_right";
    public static final String PAD_CHAR = "pad_char";
    private Map<String, PropertyFormat> formatMap;
    private PropertyFormat defaultFormat;


    public PropertyFormatInfo(Map<String, PropertyFormat> formatMap) {
        this(formatMap, new PropertyFormat());
    }

    /**
     * Create an instance using a map of columns formats and a default format to use for undeclared columns.
     *
     * @param formatMap
     * @param defaultFormat default format for undeclared columns. It does not have impact on formatting rules
     *                      defined in formatMap.
     */
    public PropertyFormatInfo(Map<String, PropertyFormat> formatMap, PropertyFormat defaultFormat) {
        this.formatMap = new HashMap<String, PropertyFormat>(formatMap);
        this.defaultFormat = defaultFormat;
    }

    /**
     * Returns format for a specified property.
     *
     * @param propertyName property name.
     * @return format for a specified property.
     */
    public PropertyFormat getPropertyFormat(String propertyName) {
        return formatMap.get(propertyName);
    }

    Map<String, PropertyFormat> getFormatMap() {
        return formatMap;
    }

    public boolean isEmpty() {
        return formatMap.isEmpty();
    }

    public PropertyFormat getDefaultFormat() {
        return defaultFormat;
    }

    public static PropertyFormatInfo parse(ConnectionParameters params, String prefix) {
        return parse(new TypedPropertiesSource(params.getProperties()), prefix);
    }

    /**
     * Creates a {@link PropertyFormatInfo} from specified properties.
     * The properties file has the following structure:
     * <pre>
     *     [prefix]propertyName.key=value
     * </pre>
     *
     * @param properties properties defining formatting.
     * @param prefix     prefix for recognizing formatting properties.
     * @return {@link PropertyFormatInfo} with a specified column formatting.
     */
    public static PropertyFormatInfo parse(TypedPropertiesSource properties, String prefix) {
        Map<String, PropertyFormat> map = new LinkedHashMap<String, PropertyFormat>();

        //For null_string, fall back to non-prefix property(1.0 compatibility)
        String nullString = null;
        if (prefix != null) {
            nullString = properties.getStringProperty(prefix + NULL_STRING);
        }
        if (nullString == null) {
            nullString = properties.getStringProperty(NULL_STRING);
        }
        PropertyFormat defaultFormat = new PropertyFormat();
        defaultFormat.setNullString(nullString);
        setProperty(defaultFormat, TRIM, TRIM, properties);
        setProperty(defaultFormat, PAD_LEFT, PAD_LEFT, properties);
        setProperty(defaultFormat, PAD_RIGHT, PAD_LEFT, properties);
        setProperty(defaultFormat, PAD_CHAR, PAD_CHAR, properties);

        for (String key : properties.getKeys()) {
            if (isPrefixed(key, prefix)) {
                String cleanKey = removePrefix(key, prefix);
                //property name determined by the last component
                int dotPos = cleanKey.lastIndexOf('.');
                if (dotPos > 0) {
                    String columnName = cleanKey.substring(0, dotPos);
                    String columnProp = cleanKey.substring(dotPos + 1);
                    PropertyFormat ci = map.get(columnName);
                    if (ci == null) {
                        ci = new PropertyFormat();
                        //Copy defaults(can be overridden later)
                        ci.setNullString(defaultFormat.getNullString());
                        ci.setTrim(defaultFormat.isTrim());
                        map.put(columnName, ci);
                    }
                    setProperty(ci, columnProp, key, properties);
                }
            }
        }

        return new PropertyFormatInfo(map, defaultFormat);
    }

    protected static void setProperty(PropertyFormat f, String columnPropName, String key, TypedPropertiesSource ps) {
        if (PATTERN.equalsIgnoreCase(columnPropName)) {
            f.setPattern(ps.getStringProperty(key));
        } else if (NULL_STRING.equalsIgnoreCase(columnPropName)) {
            f.setNullString(ps.getStringProperty(key));
        } else if (LOCALE.equalsIgnoreCase(columnPropName)) {
            f.setLocale(ps.getLocaleProperty(key));
        } else if (TRIM.equalsIgnoreCase(columnPropName)) {
            f.setTrim(ps.getBooleanProperty(key, false));
        } else if (TYPE.equalsIgnoreCase(columnPropName)) {
            f.setType(ps.getStringProperty(key));
        } else if (CLASS_NAME.equalsIgnoreCase(columnPropName)) {
            f.setClassName(ps.getStringProperty(key));
        } else if (PAD_LEFT.equalsIgnoreCase(columnPropName)) {
            f.setPadLeft(ps.getNumberProperty(key, 0).intValue());
        } else if (PAD_RIGHT.equalsIgnoreCase(columnPropName)) {
            f.setPadRight(ps.getNumberProperty(key, 0).intValue());
        } else if (PAD_CHAR.equalsIgnoreCase(columnPropName)) {
            String v = ps.getStringProperty(key);
            f.setPadChar(v == null || v.length() == 0 ? ' ' : v.charAt(0));
        }
    }

    /**
     * Creates an empty format without columns.
     *
     * @return an empty format without columns.
     */
    public static PropertyFormatInfo createEmpty() {
        final Map<String, PropertyFormat> map = Collections.emptyMap();
        return new PropertyFormatInfo(map);
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

    @Override
    public String toString() {
        return "PropertyFormatInfo{default=" + defaultFormat + ", map=" + formatMap + "}";
    }
}
