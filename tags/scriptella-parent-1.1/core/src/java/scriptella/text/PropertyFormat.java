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
 * Holds formatting/parsing rules for a property.
 *
 * @author Fyodor Kupolov
 * @since 1.1
 */
public class PropertyFormat {
    private String pattern;
    private Format format;
    private boolean trim;
    private String nullString;
    private Locale locale;
    private String type;
    private String className;
    private int padLeft;
    private int padRight;
    private char padChar=' ';

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
            ValueFormatBuilder b = new ValueFormatBuilder();
            b.setClassName(className).setType(type).setPattern(pattern).setLocale(locale);
            format = b.build();
        }
        return format;
    }


    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public boolean isTrim() {
        return trim;
    }

    public void setTrim(boolean trim) {
        this.trim = trim;
    }

    public String getClassName() {
        return className;
    }

    /**
     * Sets class name of a custom formatter (subclass of java.text.Format).
     *
     * @param className class name of a custom formatter
     */
    public void setClassName(String className) {
        this.className = className;
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

    public int getPadLeft() {
        return padLeft;
    }

    public void setPadLeft(int padLeft) {
        this.padLeft = padLeft;
    }

    public int getPadRight() {
        return padRight;
    }

    public void setPadRight(int padRight) {
        this.padRight = padRight;
    }

    public char getPadChar() {
        return padChar;
    }

    public void setPadChar(char padChar) {
        this.padChar = padChar;
    }

    public Object parse(final String value) {
        if (value == null) {
            return null;
        }
        String parseValue = trim ? value.trim() : value;
        if (parseValue.equals(nullString)) {
            return null;
        }
        Object result = parseValue;
        if (getFormat() != null) {
            try {
                //For numbers always trim the value, otherwise DecimalFormat will through an exception
                if ("number".equals(type)) {
                    parseValue = parseValue.trim();
                }
                result = getFormat().parseObject(parseValue);
                //unwrap the array if necessary
                if (result instanceof Object[]) {
                    Object[] arr = (Object[]) result;
                    result = (arr.length == 1) ? arr[0] : arr;
                }

            } catch (ParseException e) {
                throw new IllegalArgumentException("Value \"" + parseValue + "\" cannot be parsed using pattern " + pattern, e);
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
            Object param = object;
            final Format format = getFormat();
            //Wrap a single object into an array if MessageFormat
            if (format instanceof MessageFormat && !(object instanceof Object[])) {
                param = new Object[]{object};
            }
            result = format.format(param);
        } else {
            result = object.toString();
        }
        if (result == null) {
            result = nullString;
        }
        //str=null means null_string is null - do not trim or pad in this case, it can be undefined variable
        if (result == null) {
            return result;
        }

        //trim and pad the result
        result =  trim ? result.trim() : result;
        return pad(result);
    }

    private String pad(String str) {
        boolean left = padLeft > 0;
        int width = left ? padLeft : padRight;
        return width == 0 ? str : StringUtils.pad(str, left, width, padChar);
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
