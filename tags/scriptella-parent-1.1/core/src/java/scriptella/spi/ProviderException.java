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

import scriptella.core.SystemException;

import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Thrown by connection provider to indicate any kind of failure.
 * <p>Service Providers must provide subclasses of this exception.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class ProviderException extends SystemException {
    private Set<String> errorCodes = new LinkedHashSet<String>();
    private String errorStatement;

    public ProviderException() {
    }

    public ProviderException(String message) {
        super(message);
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderException(Throwable cause) {
        super(cause);
    }

    /**
     * Returns error codes attached to this exception. For example
     * JDBC drivers reports 2 error codes: SQLSTATE and vendor code.
     * <p>Do not use &quot;<b>, . ;</b>&quot; in error codes.
     *
     * @return set of error codes.
     */
    public Set<String> getErrorCodes() {
        return errorCodes;
    }

    /**
     * Adds error code to this exception.
     *
     * @param errorCode vendor specific error code.
     * @return this exception for convenience.
     */
    public ProviderException addErrorCode(String errorCode) {
        errorCodes.add(errorCode);
        return this;
    }

    /**
     * Sets problem statement which caused this exception/
     *
     * @param errStmt statement text.
     * @return this exception for convenience.
     */
    protected ProviderException setErrorStatement(String errStmt) {
        this.errorStatement = errStmt;
        return this;
    }

    /**
     * This method should be overriden by providers relying on external APIs to work with connections.
     * Used only for informative error reporting.
     * <p>Examples: SQL Exceptions, LDAP connection exceptions etc.
     *
     * @return external API throwable wich may be important for user to recognize the problem.
     */
    public Throwable getNativeException() {
        return getCause();
    }

    /**
     * Returns a statement for this error if any.
     *
     * @return statement text and additional data.
     */
    public String getErrorStatement() {
        return errorStatement;
    }

    /**
     * Overriden by subclasses to provide user friendly provider name.
     *
     * @return provider name.
     */
    public abstract String getProviderName();

    public String toString() {
        StringBuilder res = new StringBuilder(super.toString());
        String es = getErrorStatement();
        if (es != null) {
            res.append(". Error statement: ").append(es);
        }
        Set<String> codes = getErrorCodes();
        if (codes != null && !codes.isEmpty()) {
            res.append(". Error codes: ").append(codes);
        }
        return res.toString();

    }


}
