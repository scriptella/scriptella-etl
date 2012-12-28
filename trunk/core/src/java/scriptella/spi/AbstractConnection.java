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
 * A base class for connections.
 * <p>Subclassing is more safe than directly implementing {@link Connection} interface.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class AbstractConnection implements Connection {
    private DialectIdentifier dialectIdentifier;
    private boolean readonly;
    //Counter for statements use counter.statements++ to update statistics
    protected final StatementCounter counter = new StatementCounter();

    /**
     * May be used by sublasses to allow full customization
     */
    protected AbstractConnection() {
    }

    /**
     * Instantiates a connection with dialectIdentifier and connection parameters.
     * @param dialectIdentifier dialect identifier.
     * @param parameters connection parameters to use for general properties.
     */
    protected AbstractConnection(DialectIdentifier dialectIdentifier, ConnectionParameters parameters) {
        this(parameters);
        if (dialectIdentifier==null) {
            throw new IllegalArgumentException("Dialect identifier cannot be null");
        }
        this.dialectIdentifier = dialectIdentifier;
    }

    /**
     * Instantiates a connection with parameters.
     * @param parameters connection parameters to use for general properties.
     */
    protected AbstractConnection(ConnectionParameters parameters) {
        if (parameters==null) {
            throw new IllegalArgumentException("Connection parameters cannot be null");
        }
        //General Scriptella property for debugging non transactional providers
        readonly=parameters.getBooleanProperty("readonly");
    }


    public DialectIdentifier getDialectIdentifier() {
        return dialectIdentifier;
    }

    protected void setDialectIdentifier(DialectIdentifier dialectIdentifier) {
        this.dialectIdentifier = dialectIdentifier;
    }

    public long getExecutedStatementsCount() {
        return counter.statements; //The default implementation
    }

    /**
     * Returns readonly mode.
     * <p>readonly=true means updates must be skipped by the driver.
     * This property is configurable by readonly property of connection declaration element.
     * Drivers are not required to support this feauture.
     * @return true if connection is in readonly mode.
     */
    public boolean isReadonly() {
        return readonly;
    }

    public void commit() throws ProviderException {
    }

    public void rollback() throws ProviderException, UnsupportedOperationException {
        throw new UnsupportedOperationException("Transactions are not supported by "+toString());
    }

    public String toString() {
        String simpleName = getClass().getSimpleName();
        return simpleName.length() == 0 ? "connection" : simpleName;
    }

    /**
     * Helper class to use for executed statements counting.
     */
    public static class StatementCounter {
        /**
         * Stores number of executed statements.
         */
        public volatile long statements;
    }
}
