/*
 * Copyright 2006-2009 The Scriptella Project Team.
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstraction for {@link java.sql.Statement} and {@link java.sql.PreparedStatement}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
abstract class StatementWrapper<T extends Statement> implements Closeable {
    private static final Logger LOG = Logger.getLogger(StatementWrapper.class.getName());
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
     * Flushes any pending operations.
     *
     * @return number of rows updated
     * @throws SQLException if DB error occurs.
     */
    public int flush() throws SQLException {
        return 0;
    }

    /**
     * @see java.sql.Statement#toString()
     */
    public String toString() {
        return statement.toString();
    }

    /**
     * Helper method for executing a batch.
     *
     * @param statement statement to execute.
     * @return sum of update counts for all commands.
     * @throws SQLException if error occurs
     */
    protected static int executeBatch(Statement statement) throws SQLException {
        int result = 0;
        int[] results = statement.executeBatch();
        for (int r : results) {
            if (r > 0) {
                result += r;
            }
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Batch of " + results.length + " statements executed.");
        }
        return result;
    }


    /**
     * {@link Statement} wrapper.
     */
    static class Simple extends StatementWrapper<Statement> {
        protected final String sql;

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

    /**
     * {@link StatementWrapper} for batching.
     * <p>This instance is intended to be shared per ETL element.
     * To overcome this and additional method {@link #setSql(String)} must be called prior to calling update.
     */
    static class Batched extends StatementWrapper<Statement> {
        private int maxBatchSize;
        private int currentBatchSize;
        private String sql;

        /**
         * For testing only.
         */
        protected Batched() {
        }

        public Batched(Statement s, JdbcTypesConverter converter, int maxBatchSize) {
            super(s, converter);
            this.maxBatchSize = maxBatchSize;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        @Override
        public int update() throws SQLException {
            statement.addBatch(sql);
            currentBatchSize++;
            int result = 0;
            if (currentBatchSize >= maxBatchSize) {
                result = executeBatch();
            }
            return result;
        }

        /**
         * Executes current batch.
         *
         * @return number of rows affected.
         * @throws SQLException if error occurs
         */
        protected int executeBatch() throws SQLException {
            try {
                return executeBatch(statement);
            } finally {
                currentBatchSize = 0;
                converter.close(); //Disposing converter
            }
        }

        @Override
        protected ResultSet query() throws SQLException {
            throw new UnsupportedOperationException("Queries not supported in batch mode");
        }

        @Override
        public void clear() {
            this.sql = null;
        }

        @Override
        public int flush() throws SQLException {
            if (currentBatchSize > 0) {
                return executeBatch();
            }
            return 0;
        }

        @Override
        public void close() {
            super.close();
        }
    }


    /**
     * {@link Prepared} for batching.
     */
    static class BatchedPrepared extends Prepared {
        private int maxBatchSize;
        private int currentBatchSize;

        /**
         * For testing only.
         */
        protected BatchedPrepared() {
        }

        public BatchedPrepared(PreparedStatement s, JdbcTypesConverter converter, int maxBatchSize) {
            super(s, converter);
            this.maxBatchSize = maxBatchSize;
        }

        @Override
        public int update() throws SQLException {
            statement.addBatch();
            currentBatchSize++;
            int result = 0;
            if (currentBatchSize >= maxBatchSize) {
                result = executeBatch();
            }
            return result;
        }

        /**
         * Executes current batch.
         *
         * @return number of rows affected.
         * @throws SQLException if error occurs
         */
        protected int executeBatch() throws SQLException {
            try {
                return executeBatch(statement);
            } finally {
                currentBatchSize = 0;
                converter.close(); //Disposing converter
            }
        }

        @Override
        public void clear() {
            //Do not clear parameters, until the batch is sent
        }

        @Override
        public int flush() throws SQLException {
            if (currentBatchSize > 0) {
                return executeBatch();
            }
            return 0;
        }

        @Override
        protected ResultSet query() throws SQLException {
            throw new UnsupportedOperationException("Queries not supported in batch mode");
        }

        @Override
        public void close() {
            super.close();
        }
    }


}
