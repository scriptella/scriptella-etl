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

import scriptella.util.StringUtils;

import java.text.Format;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Holds formatting/parsing rules for a column.
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
//TODO Make formats extensible by enabling custom formats lookup via SPI.
public class ColumnFormat {
    private String pattern;
    private Format format;
    private boolean trim;
    private String nullString;
    private Locale locale;
    private String type;

    String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) throws IllegalArgumentException {
        this.pattern = pattern;
        if (pattern == null) {
            format = null;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    protected Format getFormat() {
        if (format == null && type != null) {
            StringBuilder fmt = new StringBuilder("{0,").append(type);
            if (pattern != null) {
                fmt.append(",").append(pattern);
            }
            fmt.append("}");
            format = new MessageFormat(fmt.toString(), locale == null ? Locale.getDefault() : locale);
        }
        return format;
    }


    public void setLocaleStr(String localeStr) {
        if (StringUtils.isEmpty(localeStr)) {
            this.locale = null;
        } else {
            String[] parts = localeStr.split("_");
            String lang = parts.length > 0 ? parts[0] : "";
            String country = parts.length > 1 ? parts[1] : "";
            String variant = parts.length > 2 ? parts[2] : "";
            this.locale = new Locale(lang, country, variant);
        }
    }

    public boolean isTrim() {
        return trim;
    }

    public void setTrim(boolean trim) {
        this.trim = trim;
    }

    public String getNullString() {
        return nullString;
    }

    public void setNullString(String nullString) {
        this.nullString = nullString;
    }

    Locale getLocale() {
        return locale;
    }

    public Object parse(final String value) {
        if ((value == null) || value.equals(nullString)) {
            return null;
        }
        String parseValue = trim ? value.trim() : value;
        Object result = parseValue;
        if (getFormat() != null) {
            try {
                result = getFormat().parseObject(parseValue);
                //unwrap the array if necessary
                if (result instanceof Object[]) {
                    Object[] arr = (Object[]) result;
                    result = (arr.length == 1) ? arr[0] : arr;
                }

            } catch (ParseException e) {
                throw new IllegalArgumentException("Value " + parseValue + " cannot be parsed using pattern " + pattern);
            }
        }
        return result;
    }

    public String format(Object object) {
        if (object == null) {
            return nullString;
        }
        String result;
        if ((!(object instanceof String)) && getFormat() != null) {
            //Wrap a single object into an array
            Object param = object instanceof Object[] ? object : new Object[]{object};
            result = getFormat().format(param);
        } else {
            result = object.toString();
        }
        if (result == null) {
            result = nullString;
        }
        if (result == null) {
            return result;
        }

        return trim ? result.trim() : result;
    }

    public void setProperty(String property, Object value) {
        //TODO Move ConnectionParameters.get[Type]Property methods into a helper class
        //Replace direct String conversion with calls to helper methods
        if ("pattern".equalsIgnoreCase(property)) {
            setPattern(value.toString());
        } else if ("null_string".equalsIgnoreCase(property)) {
            setNullString(value.toString());
        } else if ("locale".equalsIgnoreCase(property)) {
            setLocaleStr(value.toString());
        } else if ("trim".equalsIgnoreCase(property)) {
            setTrim(Boolean.valueOf(value.toString().trim()));
        } else if ("type".equalsIgnoreCase(property)) {
            setType(value.toString());
        }

    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (pattern != null) {
            result.append(pattern);
        }
        if (trim) {
            appendCommaSeparator(result).append("trim=true");
        }
        if (nullString != null) {
            appendCommaSeparator(result).append("nullString=").append(nullString);
        }
        if (locale != null) {
            appendCommaSeparator(result).append("locale=").append(locale);
        }
        return result.toString();
    }

    protected static StringBuilder appendCommaSeparator(StringBuilder stringBuilder) {
        if (stringBuilder.length() > 0) {
            stringBuilder.append(", ");
        }
        return stringBuilder;
    }
}
