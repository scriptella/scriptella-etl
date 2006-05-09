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
 * A base class for connections.
 * <p>Subclassing is more safe than directly implementing {@link Connection} interface.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class AbstractConnection implements Connection {
    private DialectIdentifier dialectIdentifier;

    /**
     * May be used by sublasses to allow full customization
     */
    protected AbstractConnection() {
    }

    /**
     * Instantiates a connection with dialectIdentifier.
     *
     * @param dialectIdentifier dialect identifier.
     */
    protected AbstractConnection(DialectIdentifier dialectIdentifier) {
        this.dialectIdentifier = dialectIdentifier;
    }

    public DialectIdentifier getDialectIdentifier() {
        return dialectIdentifier;
    }

    protected void setDialectIdentifier(DialectIdentifier dialectIdentifier) {
        this.dialectIdentifier = dialectIdentifier;
    }

    public void commit() throws ProviderException {
    }

    public void rollback() throws ProviderException {
    }

    public boolean isTransactable() throws ProviderException {
        return false;
    }


    public String toString() {
        String simpleName = getClass().getSimpleName();
        return simpleName.length() == 0 ? "connection" : simpleName + '{' +
                dialectIdentifier + '}';
    }
}
