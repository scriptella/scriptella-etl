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
import scriptella.spi.QueryCallback;
import scriptella.util.ExceptionUtils;
import scriptella.util.IOUtils;

import java.io.Closeable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Abstraction for {@link java.sql.Statement} and {@link java.sql.PreparedStatement}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
abstract class StatementWrapper<T extends Statement> implements Closeable {
    protected final JdbcTypesConverter converter;
    protected final T statement;

    /**
     * For testing only.
     */
    protected StatementWrapper() {
        converter = null;
        statement = null;
    }

    protected StatementWrapper(T statement, JdbcTypesConverter converter) {
        if (statement == null) {
            throw new IllegalArgumentException("statement cannot be null");
        }
        if (converter == null) {
            throw new IllegalArgumentException("converter cannot be null");
        }
        this.statement = statement;
        this.converter = converter;
    }

    /**
     * Release any resources opened by this statement.
     */
    public void close() {
        JdbcUtils.closeSilent(statement);
    }


    /**
     * Executes the given SQL statement, which may be an INSERT, UPDATE, or DELETE statement
     * or an SQL statement that returns nothing, such as an SQL DDL statement.
     *
     * @return either the row count for INSERT, UPDATE, or DELETE statements or 0 for SQL statements that return nothing.
     * @throws SQLException if JDBC driver fails to execute the operation.
     */
    public abstract int update() throws SQLException;

    /**
     * Executes the query and returns the result set.
     *
     * @return result set with query result.
     * @throws SQLException if JDBC driver fails to execute the operation.
     */
    protected abstract ResultSet query() throws SQLException;

    public void query(final QueryCallback queryCallback, final ParametersCallback parametersCallback) throws SQLException {
        ResultSetAdapter r = null;
        try {
            r = new ResultSetAdapter(query(), parametersCallback, converter);
            while (r.next()) {
                queryCallback.processRow(r);
            }
        } finally {
            IOUtils.closeSilently(r);
        }
    }

    public void setParameters(final List<Object> params) throws SQLException {
    }

    /**
     * Clears any transient state variables, e.g. statement parameters etc.
     */
    public void clear() {
    }

    /**
     * @see java.sql.Statement#toString()
     */
    public String toString() {
        return statement.toString();
    }


    /**
     * {@link Statement} wrapper.
     */
    static class Simple extends StatementWrapper<Statement> {
        private final String sql;

        /**
         * For testing only.
         */
        protected Simple(String sql) {
            this.sql = sql;
        }

        public Simple(Statement s, String sql, JdbcTypesConverter converter) {
            super(s, converter);
            this.sql = sql;
        }

        @Override
        public int update() throws SQLException {
            return statement.executeUpdate(sql);
        }

        @Override
        protected ResultSet query() throws SQLException {
            return statement.executeQuery(sql);
        }

    }

    /**
     * {@link PreparedStatement} wrapper.
     */
    static class Prepared extends StatementWrapper<PreparedStatement> {
        /**
         * For testing only.
         */
        protected Prepared() {
        }

        public Prepared(PreparedStatement s, JdbcTypesConverter converter) {
            super(s, converter);
        }

        /**
         * Sets parameters for this statement.
         * <p>Default implementation is noop.
         *
         * @param params parameters to set.
         * @throws SQLException
         */
        @Override
        public void setParameters(List<Object> params) throws SQLException {
            for (int i = 0, n = params.size(); i < n; i++) {
                Object o = params.get(i);
                converter.setObject(statement, i + 1, o);
            }
        }

        @Override
        public int update() throws SQLException {
            try {
                return statement.executeUpdate();
            } finally {
                converter.close(); //Disposing converter
            }
        }

        @Override
        protected ResultSet query() throws SQLException {
            return statement.executeQuery();
        }

        @Override
        public void clear() {
            try {
                statement.clearParameters();
            } catch (SQLException e) {
                ExceptionUtils.ignoreThrowable(e);
            }

        }

    }


}
