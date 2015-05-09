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
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
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
 * @author Sean Summers
 * @version 1.1
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
     * Name of the <code>escape</code> connection property.
     * The character to use for escaped elements when writing to files. Use empty string to suppress
     * escaping.
     */
    public static final String ESCAPE = "escape";

    /**
     * Name of the <code>headers</code> connection property.
     * true means the first line contains headers. Default value is true.
     * <p>Only valid for &lt;query&gt; elements.
     */
    public static final String HEADERS = "headers";

    /**
     * Name of the <code>quoteall</code> connection property.
     * false means only elements with the quote or escape character are quoted.
     * Default value is true.
     */
    public static final String QUOTEALL = "quoteall";

    public CsvConnection(ConnectionParameters parameters) {
        super(Driver.DIALECT, new CsvConnectionParameters(parameters));
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
        final CsvConnectionParameters csvParams = getConnectionParameters();
        ParametersCallback formattingCallback = csvParams.getPropertyFormatter().format(parametersCallback);
        final boolean trimLines = csvParams.isTrimLines();
        final boolean quoteall = csvParams.isQuoteall();
        PropertiesSubstitutor ps = new PropertiesSubstitutor(formattingCallback);
        for (String[] row; (row = reader.readNext()) != null;) {
            EtlCancelledException.checkEtlCancelled();
            for (int i = 0; i < row.length; i++) {
                String field = row[i];
                if (field != null) {
                    if (trimLines) {//removing extra whitespaces by default
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
                    out.writeNext(row, quoteall);
                    counter.statements++;
                } catch (Exception e) {
                    throw new CsvProviderException("Failed to write CSV row ", e);
                }
            }
        }
        if (csvParams.isFlush()) {
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
        final CsvConnectionParameters csvParams = getConnectionParameters();
        try {
            final CsvQuery q = newCsvQuery(queryContentReader, new PropertiesSubstitutor(parametersCallback));
            CSVReader inputCSVContentReader = new CSVReader(newInputReader(), csvParams.getSeparator(), csvParams.getQuote(), csvParams.getSkipLines());
            q.execute(inputCSVContentReader, queryCallback, counter);
        } catch (IOException e) {
            throw new CsvProviderException("Failed to open CSV file " + csvParams.getUrl() + " for input", e);
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
            final CsvConnectionParameters csvParams = getConnectionParameters();
            try {
                writer = newOutputWriter();
                out = new CSVWriter(writer, csvParams.getSeparator(), csvParams.getQuote(), csvParams.getEscape(), csvParams.getEol());
            } catch (IOException e) {
                throw new CsvProviderException("Unable to open URL " + csvParams.getUrl() + " for output", e);
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
        return new CsvQuery(csvReader, ps, getConnectionParameters());
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

    @Override
    protected CsvConnectionParameters getConnectionParameters() {
        return (CsvConnectionParameters) super.getConnectionParameters();
    }
}
