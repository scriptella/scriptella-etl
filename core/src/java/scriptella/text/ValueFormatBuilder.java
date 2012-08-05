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

import java.text.Format;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * Builder class for constructing {@link Format} instances based on the following parameters:
 * <ul>
 * <li>type - built-in type name, i.e. timestamp, number, date, time etc</li>
 * <li>className - name of the custom sub-class of {@link Format}</li>
 * <li>pattern - pattern to use for a specified type</li>
 * <li>locale - locale to use for formatting</li>
 * </ul>
 *
 * @author Fyodor Kupolov
 * @since 1.1
 */
public class ValueFormatBuilder {
    private String type;
    private String className;
    private String pattern;
    private Locale locale;

    public String getType() {
        return type;
    }

    public ValueFormatBuilder setType(String type) {
        this.type = type;
        return this;
    }

    public String getClassName() {
        return className;
    }

    public ValueFormatBuilder setClassName(String className) {
        this.className = className;
        return this;
    }

    public String getPattern() {
        return pattern;
    }

    public ValueFormatBuilder setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public Locale getLocale() {
        return locale;
    }

    public ValueFormatBuilder setLocale(Locale locale) {
        this.locale = locale;
        return this;
    }

    public Format build() {
        //if format class name is defined - instantiate it and return
        if (!StringUtils.isEmpty(className)) {
            Class<?> formatClass;
            try {
                try {
                    formatClass = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    formatClass = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
                }
                if (!Format.class.isAssignableFrom(formatClass)) {
                    throw new ConfigurationException("Specified format class " + className + " is not a subclass of " + Format.class);
                }
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Specified format class " + className + " cannot be found");
            }
            try {
                return (Format) formatClass.newInstance();
            } catch (Exception e) {
                throw new ConfigurationException("Cannot instantiate format class " + className, e);
            }
        }
        if (type == null) {
            throw new IllegalArgumentException("Type must be specified");
        }
        if (type.equalsIgnoreCase("timestamp")) {
            return new TimestampValueFormat();
        }
        StringBuilder fmt = new StringBuilder("{0,").append(type);
        if (pattern != null) {
            fmt.append(",").append(pattern);
        }
        fmt.append("}");
        return new MessageFormat(fmt.toString(), locale == null ? Locale.getDefault() : locale);
    }


}
