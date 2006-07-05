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
package scriptella.jdbc;

import scriptella.expressions.ParametersCallback;
import scriptella.spi.ThisParameter;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;


/**
 * Represents SQL query result set as {@link ParametersCallback}.
 * <p>This class exposes pseudo column <code>rownum</code> -current row number starting at 1.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ResultSetAdapter implements ParametersCallback, Closeable {
    public static final String ROWNUM = "rownum";
    private static final Pattern NUM_PTR = Pattern.compile("\\d+");
    private ResultSet resultSet;
    private Map<String, Integer> namesMap;
    private ParametersCallback params;
    private int rowNum;
    private Object[] row;

    public ResultSetAdapter(ResultSet resultSet,
                            ParametersCallback parametersCallback) {
        this.params = parametersCallback;
        this.resultSet = resultSet;
        namesMap = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);

        try {
            final ResultSetMetaData m = resultSet.getMetaData();
            final int n = m.getColumnCount();

            for (int i = 1; i <= n; i++) {
                String columnName = m.getColumnName(i);
                if (!ThisParameter.NAME.equalsIgnoreCase(columnName)) {
                    namesMap.put(columnName, i);
                }
            }
            row = new Object[n];
        } catch (SQLException e) {
            throw new JDBCException("Unable to process result set ", e);
        }
    }

    /**
     * @return true if the new current row is valid; false if there are no more rows
     * @see java.sql.ResultSet#next()
     */
    public boolean next() {
        try {
            boolean res = resultSet.next();
            rowNum++;
            Arrays.fill(row, null);
            return res;
        } catch (SQLException e) {
            throw new JDBCException("Unable to move cursor to the next row", e);
        }
    }


    public Object getParameter(final String name) {
        if (ROWNUM.equalsIgnoreCase(name)) { //return current row number
            return rowNum;
        }
        try {
            Integer index = namesMap.get(name);
            if (index == null) {
                //Check for "this" parameter
                if (ThisParameter.NAME.equalsIgnoreCase(name)) {
                    return ThisParameter.get(params);
                } else if (NUM_PTR.matcher(name).matches()) {//If name is not a column name and is integer
                    try {
                        index = Integer.valueOf(name); //Try to parse name as index
                    } catch (NumberFormatException e) {
                    }
                }
            }
            if (index != null) { //if index found
                int ind = index-1;
                if (row[ind] == null) { //cache miss
                    row[ind] = resultSet.getObject(ind+1);
                }
                return row[ind];
            } else { //otherwise call uppper level params
                return params.getParameter(name);
            }

        } catch (SQLException e) {
            throw new JDBCException("Unable to get parameter " + name, e);
        }
    }

    /**
     * Closes the underlying resultset.
     * <p>This method should operate without raising exceptions.
     */
    public void close() {
        if (resultSet != null) {
            JDBCUtils.closeSilent(resultSet);
            resultSet = null;
            row = null;
        }
    }
}
