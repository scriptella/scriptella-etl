/*
 * Copyright 2006-2007 The Scriptella Project Team.
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

import scriptella.core.EtlCancelledException;
import scriptella.driver.csv.opencsv.CSVReader;
import scriptella.driver.csv.opencsv.CSVWriter;
import scriptella.driver.text.AbstractTextConnection;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
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
public class  CsvConnection extends AbstractTextConnection {
    private static final Logger LOG = Logger.getLogger(CsvConnection.class.getName());
    private CSVWriter out;
    private Writer writer;


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
     * Name of the <code>headers</code> connection property.
     * true means the first line contains headers. Default value is true.
     * <p>Only valid for &lt;query&gt; elements.
     */
    public static final String HEADERS = "headers";

    /**
     * Name of the <code>null_string</code> connection property.
     * If set, specifies value of a string token to be parsed as Java <code>null</code> literal.
     */
    public static final String NULL_STRING = "null_string";


    private final char separator;
    private final char quote;
    private final boolean headers;
    private final String nullString;

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
        nullString = parameters.getStringProperty(NULL_STRING);
    }

    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ProviderException {
        try {
            executeScript(scriptContent.open(), parametersCallback);
            if (flush && writer != null) {
                writer.flush();
            }
        } catch (IOException e) {
            throw new CsvProviderException("Cannot read script.", e);
        }
    }


    void executeScript(Reader reader, ParametersCallback parametersCallback) throws IOException {

        CSVReader r = new CSVReader(reader);//Parsing rules of script in XML is always standard
        CSVWriter out = getOut();
        PropertiesSubstitutor ps = new PropertiesSubstitutor(parametersCallback);
        for (String[] row; (row = r.readNext()) != null;) {
            EtlCancelledException.checkEtlCancelled();
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
            if (row.length == 1 && StringUtils.isAsciiWhitespacesOnly(row[0])) {
                continue;
            }

            if (isReadonly()) {
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.info("Readonly Mode - " + Arrays.deepToString(row) + " has been skipped.");
                }
            } else {
                try {
                    out.writeNext(row);
                    counter.statements++;
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
        CSVReader queryReader;
        try {
            queryReader = new CSVReader(queryContent.open());
        } catch (IOException e) {
            throw new CsvProviderException("Failed to read query content", e);
        }
        CsvQuery q = new CsvQuery(queryReader, new PropertiesSubstitutor(parametersCallback), nullString, headers, trim);
        try {
            q.execute(new CSVReader(newInputReader(), separator, quote, skipLines), queryCallback, counter);
        } catch (IOException e) {
            throw new CsvProviderException("Failed to open CSV file " + url + " for input", e);
        }
    }

    /**
     * @return lazily intialized writer.
     */
    protected CSVWriter getOut() {
        if (out == null) {
            try {
                writer = newOutputWriter();
                out = new CSVWriter(writer, separator, quote, eol);
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
                writer = null;
            } catch (Exception e) {
                LOG.log(Level.INFO, "A problem occured while trying to close CSV writer", e);
            }
        }
    }
}
