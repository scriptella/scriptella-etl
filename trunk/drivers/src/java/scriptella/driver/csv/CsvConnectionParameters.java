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

package scriptella.driver.csv;

import scriptella.driver.csv.opencsv.CSVWriter;
import scriptella.driver.text.TextConnectionParameters;
import scriptella.spi.ConnectionParameters;

/**
 * Connection parameters for CSV driver
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class CsvConnectionParameters extends TextConnectionParameters {
    public static final char DEFAULT_SEPARATOR = ',';
    public static final char DEFAULT_QUERY = '\"';
    public static final boolean DEFAULT_HEADERS = true;
    protected char separator;
    protected char quote;
    protected boolean headers;

    protected CsvConnectionParameters() {
        separator = DEFAULT_SEPARATOR;
        quote = DEFAULT_QUERY;
        headers = DEFAULT_HEADERS;
    }

    public CsvConnectionParameters(ConnectionParameters parameters) {
        super(parameters);
        String sep = parameters.getStringProperty(CsvConnection.SEPARATOR);
        if (sep != null && sep.length() > 0) {
            separator = sep.charAt(0);
        } else {
            separator = DEFAULT_SEPARATOR;
        }
        String q = parameters.getStringProperty(CsvConnection.QUOTE);
        if (q == null) {
            quote = DEFAULT_QUERY; //default value
        } else if (q.length() > 0) {
            quote = q.charAt(0);
        } else { //otherwise no quoting
            quote = CSVWriter.NO_QUOTE_CHARACTER;
        }

        headers = parameters.getBooleanProperty(CsvConnection.HEADERS, DEFAULT_HEADERS);
    }

    public char getSeparator() {
        return separator;
    }

    public char getQuote() {
        return quote;
    }

    public boolean isHeaders() {
        return headers;
    }

    public void setSeparator(char separator) {
        this.separator = separator;
    }

    public void setQuote(char quote) {
        this.quote = quote;
    }

    public void setHeaders(boolean headers) {
        this.headers = headers;
    }
}
