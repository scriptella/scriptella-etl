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
import scriptella.driver.text.AbstractTextConnection;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.IOUtils;
import scriptella.util.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a connection to CSV file.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class CsvConnection extends AbstractTextConnection {
    private static final Logger LOG = Logger.getLogger(CsvConnection.class.getName());
    private CSVWriter out;


    /**
     * Name of the <code>separator</code> connection property.
     * The delimiter to use for separating entries when reading from or writing to files.
     */
    public static final String SEPARATOR = "separator";
    /**
     * Name of the <code>quote</code> connection property.
     * The character to use for quoted elements when reading from or writing to files. Use empty string to suppress
     * quoting.
     */
    public static final String QUOTE = "quote";

    /**
     * Name of the <code>suppressHeaders</code> connection property.
     * true means the first line contains headers. Default value is true.
     * <p>Only valid for &lt;query&gt; elements.
     */
    public static final String HEADERS = "headers";

    private final char separator;
    private final char quote;
    private final boolean headers;


    public CsvConnection(ConnectionParameters parameters) {
        super(Driver.DIALECT, parameters);
        String sep = parameters.getStringProperty(SEPARATOR);
        if (sep != null && sep.length() > 0) {
            separator = sep.charAt(0);
        } else {
            separator = ',';
        }
        String q = parameters.getStringProperty(QUOTE);
        if (q == null) {
            quote = '\"'; //default value
        } else if (q.length() > 0) {
            quote = q.charAt(0);
        } else { //otherwise no quoting
            quote = CSVWriter.NO_QUOTE_CHARACTER;
        }

        headers = parameters.getBooleanProperty(HEADERS, true);
    }

    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ProviderException {
        try {
            executeScript(scriptContent.open(), parametersCallback);
        } catch (IOException e) {
            throw new CsvProviderException("Cannot read script.", e);
        }


    }

    void executeScript(Reader reader, ParametersCallback parametersCallback) throws IOException {

        CSVReader r = new CSVReader(reader);//Parsing rules of script in XML is always standard
        CSVWriter out = getOut();
        PropertiesSubstitutor ps = new PropertiesSubstitutor(parametersCallback);
        for (String[] row; (row = r.readNext()) != null;) {
            for (int i = 0; i < row.length; i++) {
                String field = row[i];
                if (field != null) {
                    if (trim) {//removing extra whitespaces by default
                        field = field.trim();
                    }
                    row[i] = ps.substitute(field);
                }
            }
            //If only one column and empty - skip this row
            if (row.length==1 && StringUtils.isAsciiWhitespacesOnly(row[0])) {
                continue;
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
            CsvQuery q = new CsvQuery(in, headers, trim);
            try {
                q.execute(new CSVReader(queryContent.open()), parametersCallback, queryCallback);
            } catch (IOException e) {
                throw new CsvProviderException("Cannot read query " + queryContent, e);
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
