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
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.util.ColumnsMap;
import scriptella.util.ExceptionUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
     * @param queryReader query content reader. Closed after this method completes.
     * @param parametersCallback parameters to use.
     * @param queryCallback callback to use for result set iteration.
     * @throws IOException if IO error occurs.
     */
    public void execute(CSVReader queryReader, ParametersCallback parametersCallback, QueryCallback queryCallback) throws IOException {
        try {
            substitutor.setParameters(parametersCallback);
            List<Pattern[]> patterns = compileQueries(queryReader);
            columnsMap = new ColumnsMap();
            if (headers) {
                String[] row = reader.readNext();
                for (int i = 0; i < row.length; i++) {
                    columnsMap.registerColumn(row[i], i + 1);
                }
            }
            //For each row
            while ((row = reader.readNext()) != null) {
                if (matches(patterns, row)) {
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

    }

    /**
     * Checks if specified row matches any of the specified patterns.
     *
     * @param patterns regexp patterns to check.
     *                 In case of patterns==null row matches criteria.
     * @param row      CSV row.
     * @return true if row matches one of queries.
     */
    static boolean matches(List<Pattern[]> patterns, String[] row) {
        //Checking border conditions
        if (patterns == null || patterns.isEmpty() || row == null || row.length == 0) {
            return true;
        }
        for (Pattern[] rowPatterns : patterns) {
            boolean rowMatches = true;
            for (int i = 0; i < rowPatterns.length; i++) {
                Pattern columnPtr = rowPatterns[i];
                if (i >= row.length) { //If patterns length exceeds row columns count
                    rowMatches = false;
                    break; //consider no matches
                }
                if (columnPtr != null && !columnPtr.matcher(row[i]).matches()) {
                    rowMatches = false;
                    break;
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
     * @return list of compiled columns.
     */
    @SuppressWarnings("unchecked")
    List<Pattern[]> compileQueries(final CSVReader r) {
        List<String[]> list;
        try {
            list = r.readAll();
        } catch (IOException e) {
            throw new CsvProviderException("Unable to read CSV query", e);
        }
        List<Pattern[]> res = null;
        for (String[] columns : list) {
            Pattern[] patterns = null;
            for (int i = 0; i < columns.length; i++) {
                String s = trim(columns[i]);
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

        return res;

    }

    /**
     * Trims string if {@link #trim} flag is true.
     *
     * @param s string to trim. May be null.
     * @return possibly trimmed string or null.
     */
    private String trim(String s) {
        return (trim && s != null) ? s.trim() : s;
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
