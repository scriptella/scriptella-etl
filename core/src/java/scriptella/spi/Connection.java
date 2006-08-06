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
package scriptella.spi;

/**
 * Represents a connection to the system provided by {@link ScriptellaDriver}.
 * <p>For most cases {@link AbstractConnection} may be used as a base for driver connection implementation.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public interface Connection {
    /**
     * @return dialect identifier for this connection.
     */
    DialectIdentifier getDialectIdentifier() throws ProviderException;

    /**
     * Executes a script specified by its content.
     * <p>scriptContent may be used as a key for caching purposes, i.e.
     * provider may precompile scripts and use compiled versions for subsequent executions.
     *
     * @param scriptContent      script content.
     * @param parametersCallback callback to get parameter values.
     */
    void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ProviderException;

    /**
     * Executes a query specified by its content.
     * <p/>
     *
     * @param queryContent       query content.
     * @param parametersCallback callback to get parameter values.
     * @param queryCallback      callback to call for each result set element produced by this query.
     * @see #executeScript(scriptella.spi.Resource, scriptella.spi.ParametersCallback)
     */
    void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException;

    /**
     * Commits a current transaction (if any).
     */
    void commit() throws ProviderException;

    /**
     * Rolls back a current transaction (if any).
     * @throws ProviderException if driver fails to roll back a transaction.
     * @throws UnsupportedOperationException if transactions are not supported
     */
    void rollback() throws ProviderException, UnsupportedOperationException;


    /**
     * Closes the connection and releases all related resources.
     */
    void close() throws ProviderException;

     /**
     * @return meaningful label for connection
     */
    String toString();
}
