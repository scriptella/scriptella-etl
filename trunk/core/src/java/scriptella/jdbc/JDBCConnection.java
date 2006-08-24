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

import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.DialectIdentifier;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JDBCConnection extends AbstractConnection {
    public static final String STATEMENT_CACHE_SIZE_KEY = "statement.cache.size";
    private Connection con;
    private static final Logger LOG = Logger.getLogger(JDBCConnection.class.getName());
    private boolean transactable = false;
    private StatementCache statementCache;
    private ParametersParser parametersParser;

    public JDBCConnection(Connection con, ConnectionParameters parameters) {
        if (con==null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.con = con;
        init(parameters);
        try {
            con.setAutoCommit(false);
        } catch (SQLException e) {
            throw new JDBCException("Unable to set autocommit=false", e);
        }
        try {
            transactable = Connection.TRANSACTION_NONE != con.getTransactionIsolation();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Unable to determine transaction isolation level for connection " + toString(), e);
        }
    }

    /**
     * Called in constructor
     */
    protected void init(ConnectionParameters parameters) {
        String cacheSizeStr = parameters.getProperty(STATEMENT_CACHE_SIZE_KEY);
        int statementCacheSize=100;
        if (cacheSizeStr!=null && cacheSizeStr.trim().length()>0) {
            try {
                statementCacheSize=Integer.valueOf(cacheSizeStr);
            } catch (NumberFormatException e) {
                throw new JDBCException(STATEMENT_CACHE_SIZE_KEY+" property must be a non negative integer",e);
            }
        }

        if (statementCacheSize>0) {
            statementCache=new StatementCache(statementCacheSize);
        }
        parametersParser =new ParametersParser(parameters.getContext());
    }

    public DialectIdentifier getDialectIdentifier() {
        try {
            final DatabaseMetaData metaData = con.getMetaData();
            if (metaData != null) { //Several drivers violate spec and return null
                return new DialectIdentifier(metaData.getDatabaseProductName(),
                        metaData.getDatabaseProductVersion());
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to obtain meta data for connection. No dialect checking." + con, e);
        }
        return DialectIdentifier.NULL_DIALECT;
    }

    /**
     * Sets common parameters specified in connection element.
     * @param element Script/Query element.
     */
    private void prepare(SQLSupport element) {
        element.setStatementCache(statementCache);
        element.setParametersParser(parametersParser);
    }

    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) {
        Script s = new Script(scriptContent);
        prepare(s);
        s.execute(con, parametersCallback);
    }

    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) {
        Query q = new Query(queryContent);
        prepare(q);
        q.execute(con, parametersCallback, queryCallback);
    }

    public void commit() {
        if (con == null) {
            throw new IllegalStateException("Attempt to commit a transaction on a closed connection");
        }
        if (!transactable) {
            LOG.log(Level.INFO, "Connection " + toString() + " doesn't support transactions. Commit ignored.");
        } else {
            try {
                con.commit();
            } catch (Exception e) {
                throw new JDBCException("Unable to commit transaction", e);
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
                throw new JDBCException("Unable to roll back transaction", e);
            }
        }
    }

    public void close() {
        if (con != null) {
            try {
                con.close();
                con = null;
            } catch (SQLException e) {
                throw new JDBCException("Unable to close a connection", e);
            }
            if (statementCache!=null) {
                statementCache.close();
                statementCache=null;
            }

        }
    }

    public Connection getNativeConnection() {
        return con;
    }

    public String toString() {
        return "JDBCConnection{" + (con == null ? "" : con.getClass()) + '}';
    }
}
