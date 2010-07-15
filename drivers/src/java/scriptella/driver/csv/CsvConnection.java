/*
 * Copyright 2006-2010 The Scriptella Project Team.
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
import scriptella.spi.*;
import scriptella.util.StringUtils;

import java.io.IOException;
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
public class CsvConnection extends AbstractTextConnection {
    protected static final Logger LOG = Logger.getLogger(CsvConnection.class.getName());
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

    protected final char separator;
    protected final char quote;
    protected final boolean headers;

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
        final CSVReader r;
        try {
            r = getScriptingElementReader(scriptContent);
        } catch (IOException e) {
            throw new CsvProviderException("Cannot open CSV script content.", e);
        }
        try {
            executeScript(r, parametersCallback);
        } catch (IOException e) {
            throw new CsvProviderException("Cannot output CSV script.", e);
        }
    }


    void executeScript(CSVReader reader, ParametersCallback parametersCallback) throws IOException {
        CSVWriter out = getOut();
        PropertiesSubstitutor ps = new PropertiesSubstitutor(parametersCallback);
        ps.setNullString(nullString);
        for (String[] row; (row = reader.readNext()) != null;) {
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
        if (flush) {
            writer.flush();
        }
    }

    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException {
        if (out != null) {
            throw new CsvProviderException("Cannot query and update a CSV file simultaneously.");
        }
        final CSVReader queryContentReader;
        try {
            queryContentReader = getScriptingElementReader(queryContent);
        } catch (IOException e) {
            throw new CsvProviderException("Failed to read query content", e);
        }
        try {
            final CsvQuery q = newCsvQuery(queryContentReader, new PropertiesSubstitutor(parametersCallback));
            CSVReader inputCSVContentReader = new CSVReader(newInputReader(), separator, quote, skipLines);
            q.execute(inputCSVContentReader, queryCallback, counter);
        } catch (IOException e) {
            throw new CsvProviderException("Failed to open CSV file " + url + " for input", e);
        }
    }

    private CSVReader getScriptingElementReader(Resource content) throws IOException {
        return new CSVReader(content.open());
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

    /**
     * Template factory method to instantiate query handlers.
     *
     * @param csvReader CSV reader to use for parsing.
     * @param ps        properties substitutor.
     * @return constructed query for a specified input and parameters.
     */
    protected CsvQuery newCsvQuery(CSVReader csvReader, PropertiesSubstitutor ps) {
        return new CsvQuery(csvReader, ps, nullString, headers, trim);
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
