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

import au.com.bytecode.opencsv.CSVReader;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.util.ColumnsMap;
import scriptella.util.ExceptionUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Query for CSV file.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class CsvQuery implements ParametersCallback, Closeable {
    private CSVReader reader;
    private final boolean headers;
    private final boolean trim;
    private ColumnsMap columnsMap;
    private String[] row;
    private Pattern[][] patterns;
    private Matcher[][] matchers;
    private PropertiesSubstitutor substitutor = new PropertiesSubstitutor();

    /**
     * Creates a query for CSVReader.
     *
     * @param reader  CSV reader.
     * @param headers true if first line contains headers.
     * @param trim    true if if extra whitespaces in the query should be trimmed.
     */
    public CsvQuery(CSVReader reader, boolean headers, boolean trim) {
        if (reader == null) {
            throw new IllegalArgumentException("CSV reader cannot be null");
        }
        this.reader = reader;
        this.headers = headers;
        this.trim = trim;
    }

    /**
     * Executes a query.
     *
     * @param queryReader        query content reader. Closed after this method completes.
     * @param parametersCallback parameters to use.
     * @param queryCallback      callback to use for result set iteration.
     * @param counter            statements counter.
     * @throws IOException if IO error occurs.
     */
    public void execute(CSVReader queryReader, ParametersCallback parametersCallback, QueryCallback queryCallback, AbstractConnection.StatementCounter counter) throws IOException {
        try {
            substitutor.setParameters(parametersCallback);
            compileQueries(queryReader);

            columnsMap = new ColumnsMap();
            if (headers) {
                String[] row = reader.readNext();
                for (int i = 0; i < row.length; i++) {
                    columnsMap.registerColumn(row[i].trim(), i + 1);
                }
            }
            //For each row

            while ((row = trim(reader.readNext())) != null) {
                if (rowMatches()) {
                    queryCallback.processRow(this);
                }
            }
        } finally { //clean up
            try {
                queryReader.close();
            } catch (Exception e) {
                ExceptionUtils.ignoreThrowable(e);
            }
        }
        if (patterns != null) {
            counter.statements += patterns.length;
        }
    }

    /**
     * Checks if current CSV row matches any of the specified patterns.
     *
     * @return true if row matches one of queries.
     */
    boolean rowMatches() {
        //Checking border conditions
        Pattern[][] ptrs = patterns;
        int columnsCount = row.length;
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
                    String col = row[j]; //Current column value
                    if (m == null) { //create new matcher
                        m = columnPtr.matcher(col);
                        rowMatchers[j] = m;
                    } else { //reuse
                        m.reset(col);
                    }
                    if (!m.matches()) {
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
     * Compiles queries into a list of patterns.
     *
     * @param r CSV reader for query text.
     */
    @SuppressWarnings("unchecked")
    void compileQueries(final CSVReader r) {
        List<String[]> list;
        try {
            list = r.readAll();
        } catch (IOException e) {
            throw new CsvProviderException("Unable to read CSV query", e);
        }
        List<Pattern[]> res = null;
        for (String[] columns : list) {
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
     * Trims array of strings if {@link #trim} flag is true.
     * @param s array of strings.
     * @return the same array instance.
     */
    private String[] trim(String[] s) {
        if (s != null && trim) {
            for (int i = 0; i < s.length; i++) {
                if (s[i] != null) {
                    s[i] = s[i].trim();
                }
            }
        }
        return s;
    }

    public Object getParameter(final String name) {
        if (reader == null) {
            throw new IllegalStateException("CSV Resultset is closed");
        }
        Integer col = columnsMap.find(name);
        if (col != null && col > 0 && col <= row.length) { //If col is not null and in range
            return row[col - 1];
        } else { //otherwise call parent
            return substitutor.getParameters().getParameter(name);
        }
    }

    public void close() throws IOException {
        if (reader != null) {
            reader.close();
            reader = null;
            columnsMap = null;
        }
    }
}
