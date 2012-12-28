/*
 * Copyright 2006-2012 The Scriptella Project Team.
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

import scriptella.configuration.ConfigurationException;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.DialectIdentifier;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.StringUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a JDBC connection.
 * TODO Extract JDBCConnectionParameters class and JDBCStatementFactory as described in statement cache
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JdbcConnection extends AbstractConnection {
    public static final String STATEMENT_CACHE_KEY = "statement.cache";
    public static final String STATEMENT_SEPARATOR_KEY = "statement.separator";
    public static final String STATEMENT_SEPARATOR_SINGLELINE_KEY = "statement.separator.singleline";
    public static final String STATEMENT_BATCH_SIZE = "statement.batchSize";
    public static final String STATEMENT_FETCH_SIZE = "statement.fetchSize";
    public static final String KEEPFORMAT_KEY = "keepformat";
    public static final String AUTOCOMMIT_KEY = "autocommit";
    public static final String AUTOCOMMIT_SIZE_KEY = "autocommit.size";
    public static final String FLUSH_BEFORE_QUERY = "flushBeforeQuery";
    public static final String TRANSACTION_ISOLATION_KEY = "transaction.isolation";
    public static final String TRANSACTION_ISOLATION_READ_UNCOMMITTED = "READ_UNCOMMITTED";
    public static final String TRANSACTION_ISOLATION_READ_COMMITTED = "READ_COMMITTED";
    public static final String TRANSACTION_ISOLATION_REPEATABLE_READ = "REPEATABLE_READ";
    public static final String TRANSACTION_ISOLATION_SERIALIZABLE = "SERIALIZABLE";
    private Connection con;
    private static final Logger LOG = Logger.getLogger(JdbcConnection.class.getName());
    private boolean transactable;
    private boolean autocommit;
    private ParametersParser parametersParser;
    protected int statementCacheSize;
    protected int statementBatchSize;
    protected int statementFetchSize;
    protected boolean flushBeforeQuery;
    protected String separator = ";";
    protected boolean separatorSingleLine;
    protected boolean keepformat;
    protected int autocommitSize;

    private Integer txIsolation;
    private final Map<Resource, SqlExecutor> resourcesMap = new IdentityHashMap<Resource, SqlExecutor>();

    public JdbcConnection(Connection con, ConnectionParameters parameters) {
        super(parameters);
        if (con == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.con = con;
        init(parameters);
        if (txIsolation != null) {
            try {
                con.setTransactionIsolation(txIsolation);
            } catch (SQLException e) {
                throw new JdbcException("Unable to set transaction isolation level for " + toString(), e);
            }
        }

        try {
            //Several drivers return -1 which is illegal, but means no TX
            transactable = con.getTransactionIsolation() > Connection.TRANSACTION_NONE;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Unable to determine transaction isolation level for connection " + toString(), e);
        }
        if (transactable) { //only effective for transactable connections
            try {
                con.setAutoCommit(autocommit);
            } catch (Exception e) {
                throw new JdbcException("Unable to set autocommit=false for " + toString(), e);
            }
        }
    }

    /**
     * Called in constructor
     *
     * @param parameters connection parameters.
     */
    protected void init(ConnectionParameters parameters) {

        StringBuilder statusMsg = new StringBuilder();
        if (!StringUtils.isAsciiWhitespacesOnly(parameters.getUrl())) {
            statusMsg.append(parameters.getUrl()).append(": ");
        }
        statementCacheSize = parameters.getIntegerProperty(STATEMENT_CACHE_KEY, 64);
        if (statementCacheSize > 0) {
            statusMsg.append("Statement cache is enabled (cache size ").append(statementCacheSize).append("). ");
        }
        statementBatchSize = parameters.getIntegerProperty(STATEMENT_BATCH_SIZE, 0);
        if (statementBatchSize > 0) {
            statusMsg.append("Statement batching is enabled (batch size ").append(statementBatchSize).append("). ");
        }
        statementFetchSize = parameters.getIntegerProperty(STATEMENT_FETCH_SIZE, 0);
        if (statementFetchSize != 0) {
            statusMsg.append("Query statement fetching is enabled (fetch size ").append(statementFetchSize).append("). ");
        }
        String separatorStr = parameters.getStringProperty(STATEMENT_SEPARATOR_KEY);
        if (!StringUtils.isEmpty(separatorStr)) {
            separator = separatorStr.trim();
        }
        statusMsg.append("Statement separator '").append(separator).append('\'');
        separatorSingleLine = parameters.getBooleanProperty(STATEMENT_SEPARATOR_SINGLELINE_KEY, false);
        statusMsg.append(separatorSingleLine ? " on a single line. " : ". ");

        keepformat = parameters.getBooleanProperty(KEEPFORMAT_KEY, false);
        String isolationStr = parameters.getStringProperty(TRANSACTION_ISOLATION_KEY);
        if (isolationStr != null) {
            isolationStr = isolationStr.trim();
            if (TRANSACTION_ISOLATION_READ_COMMITTED.equalsIgnoreCase(isolationStr)) {
                txIsolation = Connection.TRANSACTION_READ_COMMITTED;
            } else if (TRANSACTION_ISOLATION_READ_UNCOMMITTED.equalsIgnoreCase(isolationStr)) {
                txIsolation = Connection.TRANSACTION_READ_UNCOMMITTED;
            } else if (TRANSACTION_ISOLATION_REPEATABLE_READ.equalsIgnoreCase(isolationStr)) {
                txIsolation = Connection.TRANSACTION_REPEATABLE_READ;
            } else if (TRANSACTION_ISOLATION_SERIALIZABLE.equalsIgnoreCase(isolationStr)) {
                txIsolation = Connection.TRANSACTION_SERIALIZABLE;
            } else if (StringUtils.isDecimalInt(isolationStr)) {
                txIsolation = parameters.getIntegerProperty(TRANSACTION_ISOLATION_KEY);
            } else {
                throw new ConfigurationException(
                        "Invalid " + TRANSACTION_ISOLATION_KEY + " connection property value: " + isolationStr +
                                ". Valid values are: " + TRANSACTION_ISOLATION_READ_COMMITTED + ", " +
                                TRANSACTION_ISOLATION_READ_UNCOMMITTED + ", " + TRANSACTION_ISOLATION_REPEATABLE_READ +
                                ", " + TRANSACTION_ISOLATION_SERIALIZABLE +
                                " or a numeric value according to java.sql.Connection transaction isolation constants");
            }

        }
        if (isolationStr != null) {
            statusMsg.append("Transaction isolation level: ").append(txIsolation).
                    append('(').append(isolationStr).append("). ");
        }
        autocommit = parameters.getBooleanProperty(AUTOCOMMIT_KEY);
        autocommitSize = parameters.getIntegerProperty(AUTOCOMMIT_SIZE_KEY, 0);
        statusMsg.append("Autocommit: ").append(autocommit);
        if (autocommitSize > 0) {
            statusMsg.append("(size ").append(autocommitSize).append(")");
        }
        statusMsg.append(".");
        flushBeforeQuery = parameters.getBooleanProperty(FLUSH_BEFORE_QUERY, false);
        if (flushBeforeQuery) {
            statusMsg.append("Flushing before query execution is enabled.");
        }

        LOG.fine(statusMsg.toString());


        parametersParser = new ParametersParser(parameters.getContext());
        initDialectIdentifier();
    }

    StatementCounter getStatementCounter() {
        return counter;
    }

    /**
     * Initializes dialect identifier for connection.
     * If driver doesn't support DatabaseMetaData or other problem occurs,
     * {@link DialectIdentifier#NULL_DIALECT} is used.
     * <p>May be overriden by subclasses.
     */
    protected void initDialectIdentifier() {
        try {
            final DatabaseMetaData metaData = con.getMetaData();
            if (metaData != null) { //Several drivers violate spec and return null
                setDialectIdentifier(new DialectIdentifier(metaData.getDatabaseProductName(),
                        metaData.getDatabaseProductVersion()));
            }
        } catch (Exception e) {
            setDialectIdentifier(DialectIdentifier.NULL_DIALECT);
            LOG.log(Level.WARNING, "Failed to obtain meta data for connection. No dialect checking for " + con, e);
        }
    }

    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) {
        SqlExecutor s = resourcesMap.get(scriptContent);
        if (s == null) {
            resourcesMap.put(scriptContent, s = new SqlExecutor(scriptContent, this));
        }
        s.execute(parametersCallback);
    }

    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) {
    	SqlExecutor q = resourcesMap.get(queryContent);
        if (q == null) {
            resourcesMap.put(queryContent, q = new SqlExecutor(queryContent, this));
        }
        if (flushBeforeQuery) {
        	flush();
        }
        q.execute(parametersCallback, queryCallback);
        if (q.getUpdateCount() < 0) {
            throw new JdbcException("SQL query cannot make updates");
        }
    }

    /**
     * Creates an instance of statement cache.
     *
     * @return new instance of statement cache.
     */
    protected StatementCache newStatementCache() {
        return new StatementCache(getNativeConnection(), statementCacheSize, statementBatchSize, statementFetchSize);
    }

    ParametersParser getParametersParser() {
        return parametersParser;
    }

    public void commit() {
        if (con == null) {
            throw new IllegalStateException("Attempt to commit a transaction on a closed connection");
        }

        flush();

        if (!transactable) {
            LOG.log(Level.INFO, "Connection " + toString() + " doesn't support transactions. Commit ignored.");
        } else {
            try {
                con.commit();
            } catch (Exception e) {
                throw new JdbcException("Unable to commit transaction", e);
            }
        }
    }

    public void rollback() {
        if (con == null) {
            throw new IllegalStateException("Attempt to roll back a transaction on a closed connection");
        }
        if (!transactable) {
            LOG.log(Level.INFO, "Connection " + toString() + " doesn't support transactions. Rollback ignored.");
        } else {
            try {
                con.rollback();
            } catch (Exception e) {
                throw new JdbcException("Unable to roll back transaction", e);
            }
        }
    }

    public void flush() throws ProviderException {
        //Caches for ETL element executors are flushed
        if (resourcesMap != null) {
            for (SqlExecutor executor : resourcesMap.values()) {
                try {
                    executor.cache.flush();
                } catch (SQLException e) {
                    throw new JdbcException("Unable to commit transaction - cannot flush cache", e);
                }
            }
        }
    }

    public void close() {
        if (con != null) {
            //Closing resources
            for (SqlExecutor element : resourcesMap.values()) {
                element.close();
            }
            resourcesMap.clear();
            try {
                con.close();
                con = null;
            } catch (SQLException e) {
                throw new JdbcException("Unable to close a connection", e);
            }

        }
    }

    public Connection getNativeConnection() {
        return con;
    }

    public String toString() {
        return "JdbcConnection{" + (con == null ? "" : con.getClass().getName()) + '}';
    }

}
