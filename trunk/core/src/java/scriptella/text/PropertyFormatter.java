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

import scriptella.spi.ParametersCallback;

/**
 * Provides formatting and parsing methods using a {@link PropertyFormatInfo} metadata.
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class PropertyFormatter {
    private PropertyFormatInfo formatInfo;

    public PropertyFormatter(PropertyFormatInfo formatInfo) {
        this.formatInfo = formatInfo;
    }


    /**
     * Parses a string value to an object (if mapping is defined in {@link PropertyFormatInfo}).
     * @param propertyName name of the property
     * @param value value to parse.
     * @return parsed value (if mapping is defined in {@link PropertyFormatInfo}), otherwise the unmodified value.
     */
    public Object parse(String propertyName, String value) {
        PropertyFormat format = getPropertyFormat(propertyName);
        return format.parse(value);
    }

    /**
     * Formats given object to string.
     *
     * @param propertyName name of the property. (Case-sensitive)
     * @param value value to format.
     * @return string representation of the value.
     */
    public String format(String propertyName, Object value) {
        final PropertyFormat format = getPropertyFormat(propertyName);
        return format.format(value);
    }

    public PropertyFormatInfo getFormatInfo() {
        return formatInfo;
    }

    PropertyFormat getPropertyFormat(String name) {
        if (formatInfo.isEmpty()) {
            return formatInfo.getDefaultFormat();
        }
        final PropertyFormat format = formatInfo.getPropertyFormat(name);
        return format == null ? formatInfo.getDefaultFormat() : format;
    }


    /**
     * Creates a {@link ParametersCallback} which formats parameters returned by the original callback.
     * @param parameters parameters callback to use as a source
     * @return a {@link ParametersCallback} which formats parameters returned by the original callback.
     */
    public ParametersCallback format(ParametersCallback parameters) {
        // Performance:
        // Consider returning the unmodified parameters callback if there are no columns defined and no defaults are changed
        return new FormattingCallback(this, parameters);
    }

    /**
     * Decorator which returns formatted value of the original callback.
     */
    static class FormattingCallback implements ParametersCallback {
        private PropertyFormatter formatter;
        private ParametersCallback callback;

        FormattingCallback(PropertyFormatter formatter, ParametersCallback callback) {
            this.formatter = formatter;
            this.callback = callback;
        }

        @Override
        public Object getParameter(String name) {
            final Object value = callback.getParameter(name);
            //Do not convert objects to string if no mapping provided. Not all objects have to be formatted.
            //In fact some internal objects like etl should never be converted
            if ((value != null) && (!(value instanceof String)) && (formatter.getFormatInfo().getPropertyFormat(name) == null)) {
                return value;
            }
            return formatter.format(name, value);
        }
    }


}
