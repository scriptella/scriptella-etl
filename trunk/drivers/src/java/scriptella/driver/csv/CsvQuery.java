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

import scriptella.driver.csv.opencsv.CSVReader;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.util.ColumnsMap;
import scriptella.util.ExceptionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Query for CSV file.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class CsvQuery implements ParametersCallback {
    protected static final Logger LOG = Logger.getLogger(CsvQuery.class.getName());
    protected final boolean headers;
    protected final boolean trim;
    protected final String nullString;
    private ColumnsMap columnsMap; //column_name->column_number mapping
    private String[] row;
    private Pattern[][] patterns;
    private Matcher[][] matchers;
    private PropertiesSubstitutor substitutor;

    /**
     * Creates a query for CSVReader.
     *
     * @param queryReader query CSVReader.
     * @param substitutor properties substitutor to use. The parameters for the substitutor must be set by a caller.
     * @param headers     true if first line of input CSV file contains headers.
     * @param trim        true if if extra whitespaces should be trimmed.
     * @param nullString  string to treat as NULL.
     */
    public CsvQuery(CSVReader queryReader, PropertiesSubstitutor substitutor, String nullString, boolean headers, boolean trim) {
        this.headers = headers;
        this.trim = trim;
        this.nullString = nullString;
        this.substitutor = substitutor;
        compileQueries(queryReader);
        closeSilently(queryReader);
    }

    /**
     * Executes a query over a specified text content.
     *
     * @param reader        CSV content reader.
     * @param queryCallback callback to use for result set iteration.
     * @param counter       statements counter.
     * @throws IOException if IO error occurs.
     */
    public void execute(CSVReader reader, QueryCallback queryCallback, AbstractConnection.StatementCounter counter) throws IOException {
        try {
            columnsMap = parseHeader(reader);
            String[] r;
            while ((r = (trim ? trim(reader.readNext()) : reader.readNext())) != null) {
                if (rowMatches(r)) {
                    processRow(queryCallback, r);
                }
            }
        } finally { //clean up
            closeSilently(reader);
        }
        if (patterns != null) {
            counter.statements += patterns.length;
        }
        columnsMap = null;
        row = null;
    }

    protected ColumnsMap parseHeader(CSVReader reader) throws IOException {
        final ColumnsMap columnsMap = new ColumnsMap();
        if (headers) {
            String[] row = reader.readNext();
            if (row != null) {
                for (int i = 0; i < row.length; i++) {
                    columnsMap.registerColumn(row[i].trim(), i + 1);
                }
            }
        }
        return columnsMap;
    }

    /**
     * Checks if current CSV row matches any of the specified patterns.
     *
     * @param r row to check
     * @return true if row matches one of queries.
     */
    protected boolean rowMatches(final String[] r) {
        //Checking border conditions
        Pattern[][] ptrs = patterns;
        int columnsCount = r.length;
        if (ptrs == null) {
            return true;
        } else if (columnsCount == 0) {
            return false;
        }

        for (int i = 0; i < ptrs.length; i++) {
            Pattern[] rowPatterns = ptrs[i];
            Matcher[] rowMatchers = matchers[i];
            boolean rowMatches = true;
            int patternsCount = rowPatterns.length;
            if (patternsCount > columnsCount) { //If patterns length exceeds row columns count
                continue; //Skip this query line
            }
            for (int j = 0; j < patternsCount; j++) {
                Pattern columnPtr = rowPatterns[j];
                if (columnPtr != null) {
                    Matcher m = rowMatchers[j];
                    String col = r[j]; //Current column value
                    if (m == null) { //create new matcher
                        m = columnPtr.matcher(col);
                        rowMatchers[j] = m;
                    } else { //reuse
                        m.reset(col);
                    }
                    if (!m.find()) {
                        rowMatches = false;
                        break;
                    }
                }
            }
            if (rowMatches) { //If this row matches current patterns
                return true;
            } //otherwise continue matching
        }
        return false; //no matches


    }

    /**
     * Processes the current row.
     * <p>This template method may be used for customizations by sublasses.
     *
     * @param queryCallback query callback to use.
     * @param r             row to pass as current parameters callback.
     */
    protected void processRow(QueryCallback queryCallback, String[] r) {
        this.row = r;
        queryCallback.processRow(this);
    }

    /**
     * Compiles queries into a list of patterns.
     *
     * @param r CSV reader for query text.
     */
    @SuppressWarnings("unchecked")
    void compileQueries(final CSVReader r) {
        List<String[]> patternList;
        try {
            patternList = r.readAll();
        } catch (IOException e) {
            throw new CsvProviderException("Unable to read CSV query", e);
        }
        List<Pattern[]> res = null;
        for (String[] columns : patternList) {
            Pattern[] patterns = null;
            trim(columns);
            for (int i = 0; i < columns.length; i++) {
                String s = columns[i];
                if (s != null && s.length() > 0) {
                    if (patterns == null) {
                        patterns = new Pattern[columns.length];
                    }
                    try {
                        patterns[i] = Pattern.compile(substitutor.substitute(s), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                    } catch (Exception e) {
                        throw new CsvProviderException("Illegal regular expression syntax for query", e, s);
                    }
                }
            }
            if (patterns != null) { //if the line has at least on not empty pattern
                if (res == null) {
                    res = new ArrayList<Pattern[]>();
                }
                res.add(patterns);
            }
        }
        if (res != null) {
            int len = res.size();
            Pattern[][] ptrs = res.toArray(new Pattern[len][]);
            //Create the matchers array to reuse for better performance
            Matcher[][] matchers = new Matcher[len][];
            for (int i = 0; i < len; i++) {
                matchers[i] = new Matcher[ptrs[i].length];
            }
            this.patterns = ptrs;
            this.matchers = matchers;
        }

    }

    /**
     * Trims array of strings
     *
     * @param s array of strings.
     * @return the same array instance.
     */
    private String[] trim(String[] s) {
        if (s != null) {
            for (int i = 0; i < s.length; i++) {
                if (s[i] != null) {
                    s[i] = s[i].trim();
                }
            }
        }
        return s;
    }

    public Object getParameter(final String name) {
        if (columnsMap == null) {
            throw new IllegalStateException("CSV Resultset is closed");
        }
        Integer col = columnsMap.find(name);
        if (col != null && col > 0 && col <= row.length) { //If col is not null and in range
            return getCurrentRowValueAt(col, name);
        } else { //otherwise call parent context.
            return substitutor.getParameters().getParameter(name);
        }
    }

    /**
     * Returns the value of a specified column.
     * <p>This method may be overriden to perform conversion etc
     * <br>2 parameters are passed for performance reasons, so that subclasses may decide wich one to use.
      * @param columnIndex column index
     * @param columnName column name.
     * @return value of the specified column of the current row.
     */
    protected Object getCurrentRowValueAt(int columnIndex, String columnName) {
        final String s = row[columnIndex - 1];
        return nullString != null && nullString.equals(s) ? null : s;
    }

    protected ColumnsMap getColumnsMap() {
        return columnsMap;
    }

    /**
     * Helper method to close CSV reader.
     *
     * @param reader CSV reader to close.
     */
    static void closeSilently(CSVReader reader) {
        try {
            reader.close();
        } catch (Exception e) {
            ExceptionUtils.ignoreThrowable(e);
        }
    }
}
