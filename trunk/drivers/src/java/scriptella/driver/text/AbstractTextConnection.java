/*
 * Copyright 2006 The Scriptella Project Team.
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

import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.DialectIdentifier;

import java.net.URL;

/**
 * Base class for Text/CSV connections.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class AbstractTextConnection extends AbstractConnection {
    protected final String encoding;
    protected final boolean trim;
    protected final URL url;
    protected final String eol;
    /**
     * Name of the <code>encoding</code> connection property.
     * Specifies charset encoding in text files.
     */
    public static final String ENCODING = "encoding";
    /**
     * Name of the <code>eol</code> connection property.
     * EOL suffix. Default value is \n.
     */
    public static final String EOL = "eol";

    /**
     * Name of the <code>trim</code> connection property.
     * Value of <code>true</code> specifies that the leading and trailing
     * whitespaces should be omitted.
     */
    public static final String TRIM = "trim";

    /**
     * For testing only.
     */
    protected AbstractTextConnection() {
        encoding = null;
        trim = false;
        url = null;
        eol = "\n";
    }

    /**
     * Initializes a text connection using specified properties.
     *
     * @param dialectIdentifier
     * @param parameters
     */
    protected AbstractTextConnection(DialectIdentifier dialectIdentifier, ConnectionParameters parameters) {
        super(dialectIdentifier, parameters);
        url = parameters.getResolvedUrl();
        encoding = parameters.getCharsetProperty(ENCODING);
        trim = parameters.getBooleanProperty("trim", true);
        String eolStr = parameters.getStringProperty(TextConnection.EOL);
        if (eolStr != null && eolStr.length() > 0) {
            eol = eolStr;
        } else {
            eol = "\n"; //Default value
        }

    }

    public String getEncoding() {
        return encoding;
    }

    public boolean isTrim() {
        return trim;
    }

    public URL getUrl() {
        return url;
    }

    public String getEol() {
        return eol;
    }

}
