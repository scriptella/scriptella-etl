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

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
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
    private boolean headers;
    private ColumnsMap columnsMap;
    private String[] row;
    private PropertiesSubstitutor substitutor = new PropertiesSubstitutor();

    /**
     * Creates a query for CSVReader.
     *
     * @param reader  CSV reader.
     * @param headers true if first line contains headers.
     */
    public CsvQuery(CSVReader reader, boolean headers) {
        if (reader == null) {
            throw new IllegalArgumentException("CSV reader cannot be null");
        }
        this.reader = reader;
        this.headers = headers;
    }

    public void execute(Reader queryReader, ParametersCallback parametersCallback, QueryCallback queryCallback) throws IOException {

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
     * @param reader reader for query text.
     * @return list of compiled columns.
     */
    @SuppressWarnings("unchecked")
    List<Pattern[]> compileQueries(final Reader reader) {
        CSVReader r = new CSVReader(reader);
        List<String[]> list;
        try {
            list = r.readAll();
        } catch (IOException e) {
            throw new CsvProviderException("Unable to read CSV query", e);
        }
        List<Pattern[]> res = null;
        for (String[] row : list) {
            Pattern[] patterns = new Pattern[row.length];
            boolean notEmptyPtr = false;
            for (int i = 0; i < row.length; i++) {
                String s = trim(row[i]);
                if (s == null || s.length() == 0) {
                    patterns[i] = null;
                } else {
                    notEmptyPtr = true;
                    try {
                        patterns[i] = Pattern.compile(substitutor.substitute(s));
                    } catch (Exception e) {
                        throw new CsvProviderException("Illegal regular expression syntax for query.", e, s);
                    }
                }
            }
            if (notEmptyPtr) { //if the line has at least on not empty pattern
                if (res == null) {
                    res = new ArrayList<Pattern[]>();
                }
                res.add(patterns);
            }
        }

        return res;

    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    public Object getParameter(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter name cannot be null");
        }
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
