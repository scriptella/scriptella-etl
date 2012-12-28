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

import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.DialectIdentifier;
import scriptella.text.PropertyFormatInfo;
import scriptella.util.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;

/**
 * Base class for Text/CSV connections.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class AbstractTextConnection extends AbstractConnection {
    private TextConnectionParameters connectionParameters;


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
     * Name of the <code>flush</code> connection property.
     * Value of <code>true</code> specifies that the outputted content should flushed immediately after
     * the &lt;script&gt; element completes.
     */
    public static final String FLUSH = "flush";

    /**
     * Name of the <code>skip_lines</code> connection property.
     * The number of lines to skip before start reading.
     * Default value is 0 (no lines are skipped).
     * <p>Only valid for &lt;query&gt; elements.
     */
    public static final String SKIP_LINES = "skip_lines";

    /**
     * Name of the <code>null_string</code> connection property.
     * If set, specifies value of a string token to be parsed/formatted as Java <code>null</code> literal.
     */
    public static final String NULL_STRING = PropertyFormatInfo.NULL_STRING;

    /**
     * Prefix for properties containing a definition of the column format.
     */
    public static final String FORMAT_PREFIX = "format.";

    /**
     * For testing only.
     */
    protected AbstractTextConnection() {
        connectionParameters = new TextConnectionParameters();
    }

    /**
     * Initializes a text connection using specified properties.
     *
     * @param dialectIdentifier
     * @param parameters
     */
    protected AbstractTextConnection(DialectIdentifier dialectIdentifier, ConnectionParameters parameters) {
        this(dialectIdentifier, new TextConnectionParameters(parameters));
    }

    protected AbstractTextConnection(DialectIdentifier dialectIdentifier, TextConnectionParameters parameters) {
        super(dialectIdentifier, parameters.getConnectionParameters());
        connectionParameters = parameters;
    }


    /**
     * Creates a new writer to send output to.
     *
     * @return writer for output.
     * @throws IOException if IO error occured.
     */
    protected Writer newOutputWriter() throws IOException {
        final URL url = connectionParameters.getUrl();
        final String encoding = connectionParameters.getEncoding();
        return url == null ? ConsoleAdapters.getConsoleWriter(encoding) :
                IOUtils.getWriter(IOUtils.getOutputStream(url), encoding);
    }

    /**
     * Creates a new reader for input.
     *
     * @return reader for input.
     * @throws IOException if IO error occured.
     */
    protected Reader newInputReader() throws IOException {
        final URL url = connectionParameters.getUrl();
        final String encoding = connectionParameters.getEncoding();
        return url == null ? ConsoleAdapters.getConsoleReader(encoding) :
                IOUtils.getReader(url.openStream(), encoding);
    }

    protected TextConnectionParameters getConnectionParameters() {
        return connectionParameters;
    }
}
