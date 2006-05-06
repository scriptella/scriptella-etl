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
package scriptella.sql;

import scriptella.execution.SystemException;

import java.util.ArrayList;
import java.util.List;


/**
 * Unchecked wrapper for SQLExcpetion or other JDBC related exceptions.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JDBCException extends SystemException {
    private String sql;
    private List<?> parameters;

    public JDBCException() {
    }

    public JDBCException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     * @param sql     SQL possibly caused the exception
     * @param params  SQL parameters
     */
    public JDBCException(String message, Throwable cause, String sql,
                         List params) {
        super(message, cause);
        this.sql = sql;
        this.parameters = new ArrayList<Object>(params);
    }

    public JDBCException(String message, Throwable cause) {
        super(message, cause);
    }

    public JDBCException(Throwable cause) {
        super(cause);
    }

    /**
     * @return SQL possibly caused the exception.
     */
    public String getSql() {
        return sql;
    }

    /**
     * @return parameters for {@link #getSql() SQL}.
     */
    public List<?> getParameters() {
        return parameters;
    }

    void setSql(final String sql) {
        this.sql = sql;
    }

    void setParameters(final List<?> parameters) {
        this.parameters = parameters;
    }

    public static JDBCException wrap(final Throwable cause) {
        if (cause instanceof JDBCException) {
            return (JDBCException) cause;
        } else if (cause instanceof Error) {
            throw (Error) cause;
        } else {
            return new JDBCException(cause);
        }
    }
}
