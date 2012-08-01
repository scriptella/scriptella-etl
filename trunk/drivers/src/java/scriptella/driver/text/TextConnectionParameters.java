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

package scriptella.driver.text;

import scriptella.spi.ConnectionParameters;
import scriptella.text.PropertyFormatInfo;
import scriptella.text.PropertyFormatter;

import java.net.URL;

/**
 * Connection parameters for text drivers.
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class TextConnectionParameters {
    private ConnectionParameters connectionParameters;
    private String encoding;
    private boolean flush;
    private URL url; //if null - use console
    private String eol;
    private int skipLines;
    private boolean trimLines;
    private PropertyFormatter propertyFormatter;

    protected TextConnectionParameters() {
        encoding = null;
        flush = false;
        url = null;
        eol = "\n";
        this.skipLines = 0;
        trimLines = true;
        this.propertyFormatter = new PropertyFormatter(PropertyFormatInfo.createEmpty());
    }

    public TextConnectionParameters(ConnectionParameters parameters) {
        this.connectionParameters = parameters;
        //URL can be set null, in this case console is used for reading/writing
        url = parameters.getUrl() == null ? null : parameters.getResolvedUrl();
        encoding = parameters.getCharsetProperty(AbstractTextConnection.ENCODING);
        //When printing to console - flushing is enabled
        flush = url == null || parameters.getBooleanProperty(AbstractTextConnection.FLUSH, false);
        String eolStr = parameters.getStringProperty(TextConnection.EOL);
        eol = eolStr != null ? eolStr : "\n";//Default value
        skipLines = parameters.getIntegerProperty(AbstractTextConnection.SKIP_LINES, 0);
        trimLines = parameters.getBooleanProperty(AbstractTextConnection.TRIM, true);
        propertyFormatter = new PropertyFormatter(PropertyFormatInfo.parse(parameters, AbstractTextConnection.FORMAT_PREFIX));
    }

    public ConnectionParameters getConnectionParameters() {
        return connectionParameters;
    }

    public String getEncoding() {
        return encoding;
    }

    public boolean isFlush() {
        return flush;
    }

    /**
     * Returns resolved URL for this connection.
     * <p>If null, the console is used for reading/writing.
     *
     * @return resolved URL or null.
     */
    public URL getUrl() {
        return url;
    }

    public String getEol() {
        return eol;
    }

    public int getSkipLines() {
        return skipLines;
    }

    public PropertyFormatter getPropertyFormatter() {
        return propertyFormatter;
    }

    public void setDefaultNullString(String nullString) {
        getPropertyFormatter().getFormatInfo().getDefaultFormat().setNullString(nullString);
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setFlush(boolean flush) {
        this.flush = flush;
    }

    public void setEol(String eol) {
        this.eol = eol;
    }

    public void setSkipLines(int skipLines) {
        this.skipLines = skipLines;
    }

    public boolean isTrimLines() {
        return trimLines;
    }

    public void setTrimLines(boolean trimLines) {
        this.trimLines = trimLines;
    }
}
