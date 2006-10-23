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
import scriptella.util.StringUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a JDBC connection.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JdbcConnection extends AbstractConnection {
    public static final String STATEMENT_CACHE_KEY = "statement.cache";
    public static final String STATEMENT_SEPARATOR_KEY = "statement.separator";
    public static final String STATEMENT_SEPARATOR_SINGLELINE_KEY = "statement.separator.singleline";
    private Connection con;
    private static final Logger LOG = Logger.getLogger(JdbcConnection.class.getName());
    private boolean transactable = false;
    private ParametersParser parametersParser;
    int statementCacheSize = 100;
    protected String separator = ";";
    protected boolean separatorSingleLine;
    private final Map<Resource, SqlSupport> resourcesMap = new IdentityHashMap<Resource, SqlSupport>();

    public JdbcConnection(Connection con, ConnectionParameters parameters) {
        super(parameters);
        if (con == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.con = con;
        init(parameters);
        try {
            //Several drivers return -1 which is illegal, but means no TX
            transactable = con.getTransactionIsolation() > Connection.TRANSACTION_NONE;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Unable to determine transaction isolation level for connection " + toString(), e);
        }
        if (transactable) { //only effective for transactable connections
            try {
                con.setAutoCommit(false);
            } catch (Exception e) {
                throw new JdbcException("Unable to set autocommit=false for " + toString(), e);
            }
        }
    }

    /**
     * Called in constructor
     */
    protected void init(ConnectionParameters parameters) {
        String cacheSizeStr = parameters.getProperty(STATEMENT_CACHE_KEY);
        if (!StringUtils.isEmpty(cacheSizeStr)) {
            try {
                statementCacheSize = Integer.valueOf(cacheSizeStr);
            } catch (NumberFormatException e) {
                throw new JdbcException(STATEMENT_CACHE_KEY + " property must be a non negative integer", e);
            }
        }
        String separatorStr = parameters.getProperty(STATEMENT_SEPARATOR_KEY);
        if (!StringUtils.isEmpty(separatorStr)) {
            separator=separatorStr.trim();
        }
        try {
            separatorSingleLine = parameters.getBooleanProperty(STATEMENT_SEPARATOR_SINGLELINE_KEY, false);
        } catch (ParseException e) {
            throw new JdbcException(e.getMessage());
        }

        parametersParser = new ParametersParser(parameters.getContext());
        initDialectIdentifier();
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
        Script s = (Script) resourcesMap.get(scriptContent);
        if (s == null) {
            resourcesMap.put(scriptContent, s = new Script(scriptContent, this));
        }
        s.execute(con, parametersCallback);
    }

    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) {
        Query q = (Query) resourcesMap.get(queryContent);
        if (q == null) {
            resourcesMap.put(queryContent, q = new Query(queryContent, this));
        }
        q.execute(con, parametersCallback, queryCallback);
    }

    ParametersParser getParametersParser() {
        return parametersParser;
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

    public void close() {
        if (con != null) {
            //Closing resources
            for (SqlSupport element : resourcesMap.values()) {
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
