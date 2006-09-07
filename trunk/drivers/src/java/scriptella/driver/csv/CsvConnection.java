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
package scriptella.driver.csv;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a connection to CSV file.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class CsvConnection extends AbstractConnection {
    private static final Logger LOG = Logger.getLogger(CsvConnection.class.getName());
    private CSVWriter out;
    private final URL url;


    /**
     * Name of the <code>encoding</code> connection property.
     * Specifies charset encoding of CSV files.
     */
    public static final String ENCODING = "encoding";
    /**
     * Name of the <code>separator</code> connection property.
     * The delimiter to use for separating entries
     */
    public static final String SEPARATOR = "separator";
    /**
     * Name of the <code>quotechar</code> connection property.
     * The character to use for quoted elements
     */
    public static final String QUOTE = "quote";
    /**
     * Name of the <code>suppressHeaders</code> connection property.
     * true means the first line contains headers. Default value is true.
     * Only valid for &lt;query&gt; elements.
     */
    public static final String HEADERS = "headers";
    /**
     * Name of the <code>eol</code> connection property.
     * EOL suffix. Default value is \n.
     * Only valid for &lt;script&gt; elements.
     */
    public static final String EOL = "eol";
    private final String encoding;
    private final char separator;
    private final char quote;
    private final String eol;//the line feed terminator to use
    private final boolean headers;


    public CsvConnection(ConnectionParameters parameters) {
        super(Driver.DIALECT, parameters);
        try {
            encoding = parameters.getCharsetProperty(ENCODING);
        } catch (ParseException e) {
            throw new CsvProviderException(e.getMessage());

        }
        String sep = parameters.getProperty(SEPARATOR);
        if (sep != null && sep.length() > 0) {
            separator = sep.charAt(0);
        } else {
            separator = ',';
        }
        String q = parameters.getProperty(QUOTE);
        if (q != null && q.length() > 0) {
            quote = q.charAt(0);
        } else {
            quote = '\"';
        }
        try {
            headers = parameters.getBooleanProperty(HEADERS, true);
        } catch (ParseException e) {
            throw new CsvProviderException(e.getMessage());
        }
        String eolStr = parameters.getProperty(EOL);
        if (eolStr != null && eolStr.length() > 0) {
            eol = eolStr;
        } else {
            eol = "\n"; //Default value
        }

        String urlStr = parameters.getUrl();
        if (urlStr == null) {
            throw new CsvProviderException("url connection attribute is required");
        }

        try {
            url = parameters.getContext().resolve(urlStr);
        } catch (MalformedURLException e) {
            throw new CsvProviderException("Invalid URL connection attribute value: " + urlStr, e);
        }
    }

    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ProviderException {
        try {
            executeScript(scriptContent.open(), parametersCallback);
        } catch (IOException e) {
            throw new CsvProviderException("Cannot read script.", e);
        }


    }

    void executeScript(Reader reader, ParametersCallback parametersCallback) throws IOException {
        CSVReader r = new CSVReader(reader, separator, quote);
        CSVWriter out = getOut();
        PropertiesSubstitutor ps = new PropertiesSubstitutor(parametersCallback);
        for (String[] row; (row = r.readNext()) != null;) {
            for (int i = 0; i < row.length; i++) {
                row[i] = ps.substitute(row[i]);
            }
            if (isReadonly()) {
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.info("Readonly Mode - " + Arrays.deepToString(row) + " has been skipped.");
                }
            } else {
                try {
                    out.writeNext(row);
                } catch (Exception e) {
                    throw new CsvProviderException("Failed to write CSV row ", e);
                }
            }
        }

    }

    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException {
        if (out != null) {
            throw new CsvProviderException("Cannot query and update a CSV file simultaneously.");
        }
        try {
            CSVReader in = new CSVReader(IOUtils.getReader(url.openStream(), encoding), separator, quote);
            CsvQuery q = new CsvQuery(in, headers);
            try {
                q.execute(queryContent.open(), parametersCallback, queryCallback);
            } catch (IOException e) {
                throw new CsvProviderException("Cannot read query "+queryContent, e);
            } finally {
                IOUtils.closeSilently(q);
            }
        } catch (IOException e) {
            throw new CsvProviderException("Unable to open URL " + url + " for input", e);
        }
    }

    /**
     * @return lazily intialized writer.
     */
    protected CSVWriter getOut() {
        if (out == null) {
            try {
                out = new CSVWriter(IOUtils.getWriter(IOUtils.getOutputStream(url), encoding), separator, quote, eol);
            } catch (IOException e) {
                throw new CsvProviderException("Unable to open URL " + url + " for output", e);
            }
        }
        return out;
    }


    public void close() throws ProviderException {
        if (out != null) {
            try {
                out.close();
            } catch (Exception e) {
                LOG.log(Level.INFO, "A problem occured while trying to close CSV writer", e);
            }
        }
    }
}
