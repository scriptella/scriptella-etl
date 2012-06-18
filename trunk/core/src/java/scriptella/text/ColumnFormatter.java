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
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class ColumnFormatter {
    private ColumnFormatInfo formatInfo;

    public ColumnFormatter(ColumnFormatInfo formatInfo) {
        this.formatInfo = formatInfo;
    }

    public Object parse(String columnName, String value) {
        if (value == null) {
            return null;
        }
        final ColumnFormat format = getColumn(columnName);
        String nullString = formatInfo.getNullString();
        //if no format defined - compare against null_string and return
        if (format == null) {
            return value.equals(nullString) ? null : value;
        }
        //if null_string is not defined on the level of column format
        if (format.getNullString() == null && value.equals(nullString)) {
            return null;
        }

        return format.parse(value);

    }

    public String format(String columnName, Object value) {
        final ColumnFormat format = getColumn(columnName);
        String result = null;
        if (format != null) {
            result = format.format(value);
        } else if (value != null) {
            result = value.toString();
        }
        if (result == null) {
            result = formatInfo.getNullString();
        }
        return result;
    }

    ColumnFormat getColumn(String name) {
        if (formatInfo.isEmpty()) {
            return null;
        }
        return formatInfo.getColumnInfo(name);
    }


    public ParametersCallback format(ParametersCallback parameters) {
        if (formatInfo.isEmpty()) {
            return parameters;
        }
        return new FormattingCallback(this, parameters);
    }

    static class FormattingCallback implements ParametersCallback {
        private ColumnFormatter formatter;
        private ParametersCallback callback;

        FormattingCallback(ColumnFormatter formatter, ParametersCallback callback) {
            this.formatter = formatter;
            this.callback = callback;
        }

        @Override
        public Object getParameter(String name) {
            return formatter.format(name, callback.getParameter(name));
        }
    }


}
