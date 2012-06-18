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

import scriptella.configuration.ConfigurationException;
import scriptella.util.StringUtils;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Typed view for properties.
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class TypedPropertiesSource {
    private Map<String, ?> properties;

    public TypedPropertiesSource(Map<String, ?> properties) {
        this.properties = properties;
    }


    public Set<String> getKeys() {
        return properties.keySet();
    }

    /**
     * Convenience method which returns property by name.
     *
     * @param name property name
     * @return property value
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Returns string value of the property.
     *
     * @param name property name.
     * @return property value.
     */
    public String getStringProperty(String name) {
        Object v = properties.get(name);
        return v == null ? null : v.toString();
    }

    /**
     * Returns numeric value of the property.
     * <p>Accepts decimal, hexadecimal, and octal numbers if property is String.
     *
     * @param name         property name.
     * @param defaultValue default value to use when property omitted.
     * @return numeric property value.
     * @throws scriptella.configuration.ConfigurationException
     *          if parsing failed.
     * @see Long#decode(String)
     */
    public Number getNumberProperty(String name, Number defaultValue) throws ConfigurationException {
        Object v = properties.get(name);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number) {
            return ((Number) v);
        }
        String s = v.toString().trim();
        if (s.length() == 0) {
            return defaultValue;
        }

        //For now we do not support doubles etc.
        try {
            return Long.decode(s);
        } catch (NumberFormatException e) {
            throw new ConfigurationException(name + " property must be integer.");
        }
    }

    /**
     * Parses property value as a boolean flag.
     *
     * @param name         property name.
     * @param defaultValue default value to use if connection has no such property.
     * @return boolean property value.
     * @throws ConfigurationException if property has unrecognized value.
     */
    public boolean getBooleanProperty(String name, boolean defaultValue) throws ConfigurationException {
        Object a = getProperty(name);
        if (a == null) {
            return defaultValue;
        }
        if (a instanceof Boolean) {
            return (Boolean) a;
        }
        if (a instanceof Number) {
            return ((Number) a).intValue() > 0;
        }
        String s = a.toString().trim();

        if ("true".equalsIgnoreCase(s) || "1".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s)) {
            return true;
        }

        if ("false".equalsIgnoreCase(s) || "0".equalsIgnoreCase(s) || "off".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s)) {
            return false;
        }
        throw new ConfigurationException("Unrecognized boolean property value " + a);
    }

    /**
     * Parses property value as a charset encoding name.
     *
     * @param name property name.
     * @return value of the property or null if connection has no such property.
     * @throws ConfigurationException if charset name is unsupported.
     */
    public String getCharsetProperty(String name) throws ConfigurationException {
        Object cs = getProperty(name);
        if (cs == null) {
            return null;
        }
        if (cs instanceof Charset) {
            return ((Charset) cs).name();
        }
        String enc = cs.toString().trim();
        if (!Charset.isSupported(enc)) {
            throw new ConfigurationException("Specified encoding " + enc + " is not supported. Supported encodings are " + Charset.availableCharsets().keySet());
        }
        return enc;
    }

    /**
     * Parses property value as a locale string.
     *
     * @param name property name.
     * @return locale or null if property not found or has no value
     * @see java.util.Locale#toString()
     */
    public Locale getLocaleProperty(String name) {
        String localeStr = getStringProperty(name);
        if (StringUtils.isEmpty(localeStr)) {
            return null;
        } else {
            String[] parts = localeStr.split("_");
            String lang = parts.length > 0 ? parts[0] : "";
            String country = parts.length > 1 ? parts[1] : "";
            String variant = parts.length > 2 ? parts[2] : "";
            return new Locale(lang, country, variant);
        }
    }

    public Map<String, ?> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "Properties{" + properties + '}';
    }
}
