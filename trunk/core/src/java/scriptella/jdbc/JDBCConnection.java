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

import scriptella.configuration.Resource;
import scriptella.expressions.ParametersCallback;
import scriptella.spi.AbstractConnection;
import scriptella.spi.DialectIdentifier;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;

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
    private Connection con;
    private static final Logger LOG = Logger.getLogger(JDBCConnection.class.getName());
    private boolean transactable = false;

    public JDBCConnection(Connection con) {
        if (con==null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.con = con;
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

    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) {
        Script s = new Script(scriptContent);
        s.execute(con, parametersCallback);
    }

    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) {
        Query q = new Query(queryContent);
        q.execute(con, parametersCallback, queryCallback);
    }

    public void commit() {
        if (con == null) {
            throw new IllegalStateException("Attempt to commit a transaction on a closed connection");
        }
        try {
            con.commit();
        } catch (Exception e) {
            throw new JDBCException("Unable to commit transaction", e);
        }
    }

    public void rollback() {
        if (con == null) {
            throw new IllegalStateException("Attempt to roll back a transaction on a closed connection");
        }
        try {
            con.rollback();
        } catch (Exception e) {
            throw new JDBCException("Unable to roll back transaction", e);
        }
    }

    public void close() {
        if (con != null) {
            try {
                con.close();
                con = null;
            } catch (SQLException e) {
                throw new JDBCException("Unable to close connection", e);
            }
        }
    }

    @Override
    public boolean isTransactable() throws ProviderException {
        return transactable;
    }

    public Connection getNativeConnection() {
        return con;
    }

    public String toString() {
        return "JDBCConnection{" + (con == null ? "" : con.getClass()) + '}';
    }
}
