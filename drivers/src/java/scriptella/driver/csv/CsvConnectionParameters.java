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
 * @author Sean Summers
 * @version 1.2
 */
public class CsvConnectionParameters extends TextConnectionParameters {
    public static final char DEFAULT_SEPARATOR = ',';
    public static final char DEFAULT_QUERY = '\"';
    public static final char DEFAULT_ESCAPE = '\"';
    public static final boolean DEFAULT_HEADERS = true;
    public static final boolean DEFAULT_QUOTEALL = true;
    protected char separator;
    protected char quote;
    protected char escape;
    protected boolean headers;
    protected boolean quoteall;

    protected CsvConnectionParameters() {
        separator = DEFAULT_SEPARATOR;
        quote = DEFAULT_QUERY;
        escape = DEFAULT_ESCAPE;
        headers = DEFAULT_HEADERS;
        quoteall = DEFAULT_QUOTEALL;
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
        String e = parameters.getStringProperty(CsvConnection.ESCAPE);
        if (e == null) {
            escape = DEFAULT_ESCAPE; //default value
        } else if (e.length() > 0) {
            escape = e.charAt(0);
        } else { //otherwise no quoting
            escape = CSVWriter.NO_ESCAPE_CHARACTER;
        }

        headers = parameters.getBooleanProperty(CsvConnection.HEADERS, DEFAULT_HEADERS);

        quoteall = parameters.getBooleanProperty(CsvConnection.QUOTEALL, DEFAULT_QUOTEALL);
    }

    public char getSeparator() {
        return separator;
    }

    public char getQuote() {
        return quote;
    }

    public char getEscape() {
        return escape;
    }

    public boolean isHeaders() {
        return headers;
    }

    public boolean isQuoteall() {
        return quoteall;
    }

    public void setSeparator(char separator) {
        this.separator = separator;
    }

    public void setQuote(char quote) {
        this.quote = quote;
    }

    public void setEscape(char escape) {
        this.escape = escape;
    }

    public void setHeaders(boolean headers) {
        this.headers = headers;
    }

    public void setQuoteall(boolean quoteall) {
        this.quoteall = quoteall;
    }
}
