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
package scriptella.spi;

/**
 * Represents a connection to the system provided by {@link ScriptellaDriver}.
 * <p>The implementations are not required to be thread safe.
 * <p>For most cases {@link AbstractConnection} may be used as a base for driver connection implementation.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public interface Connection {
    /**
     * This method returns a language dialect identifier for this connection.
     *
     * @return dialect identifier.
     * @throws ProviderException if driver failed to determine a language dialect.
     */
    DialectIdentifier getDialectIdentifier() throws ProviderException;

    /**
     * Executes a script specified by its content.
     * <p>scriptContent may be used as a key for caching purposes, i.e.
     * provider may precompile scripts and use compiled versions for subsequent executions.
     * Please note that <em>only inline {@link scriptella.configuration.StringResource text resources}
     * can be safely cached</em>.
     *
     * @param scriptContent      script content. Cannot be null.
     * @param parametersCallback callback to get parameter values. Cannot be null.
     * @throws ProviderException if script execution failed.
     */
    void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ProviderException;

    /**
     * Executes a query specified by its content.
     * <p/>
     *
     * @param queryContent       query content. Cannot be null.
     * @param parametersCallback callback to get parameter values. Cannot be null.
     * @param queryCallback      callback to call for each result set element produced by this query. Cannot be null.
     * @throws ProviderException if query execution failed.
     * @see #executeScript(scriptella.spi.Resource,scriptella.spi.ParametersCallback)
     */
    void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException;

    /**
     * This method returns the number of executed statements or 0 if this feature is unsupported.
     * <p>If possible the connection should collect statistics about the number of executed statement.
     * It's recommended to provide the most actual execution statistics, i.e. increment internal statements
     * counter during a script or a query execution, so the monitoring tools would be able to track progress.
     *
     * @return number of executed statements or 0 if this feature is unsupported.
     */
    long getExecutedStatementsCount();

    /**
     * Commits a current transaction (if any).
     * <p>Throwing an error during commit phase cause {@link #rollback() rollback}.
     *
     * @throws ProviderException if a problem occured during commit phase.
     */
    void commit() throws ProviderException;

    /**
     * Rolls back a current transaction (if any).
     *
     * @throws ProviderException             if driver fails to roll back a transaction.
     * @throws UnsupportedOperationException if transactions are not supported
     */
    void rollback() throws ProviderException, UnsupportedOperationException;


    /**
     * Closes the connection and releases all related resources.
     *
     * @throws ProviderException if a critical failure occured.
     */
    void close() throws ProviderException;

    /**
     * @return meaningful label for connection
     */
    String toString();
}
