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

import scriptella.spi.ParametersCallback;
import scriptella.util.ColumnsMap;
import scriptella.util.IOUtils;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


/**
 * Represents SQL query result set as {@link ParametersCallback}.
 * <p>This class exposes pseudo column <code>rownum</code> -current row number starting at 1.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ResultSetAdapter implements ParametersCallback, Closeable {

    private ResultSet resultSet;
    private ColumnsMap columnsMap;
    private ParametersCallback params; //parent parameters callback to use
    private JDBCTypesConverter converter;
    private final int columnsCount;

    /**
     * Instantiates an adapter, prepares a cache and builds a map of column names.
     *
     * @param resultSet          resultset to adapt.
     * @param parametersCallback parent parameter callback.
     * @param converter          type converter to use for getting column values as object.
     */
    public ResultSetAdapter(ResultSet resultSet,
                            ParametersCallback parametersCallback, JDBCTypesConverter converter) {
        this.params = parametersCallback;
        this.resultSet = resultSet;
        columnsMap = new ColumnsMap();
        this.converter = converter;

        try {
            final ResultSetMetaData m = resultSet.getMetaData();
            columnsCount = m.getColumnCount();

            for (int i = 1; i <= columnsCount; i++) {
                columnsMap.registerColumn(m.getColumnName(i), i);
            }
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
            return resultSet.next();
        } catch (SQLException e) {
            throw new JDBCException("Unable to move cursor to the next row", e);
        }
    }


    public Object getParameter(final String name) {
        try {
            Integer index = columnsMap.find(name);
            if (index != null && index > 0 && index <= columnsCount) { //if index found and in range
                int ind = index - 1;
                return converter.getObject(resultSet, ind + 1);
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
            IOUtils.closeSilently(converter);
            resultSet = null;
            params = null;
            columnsMap = null;
        }
    }
}
