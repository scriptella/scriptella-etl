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

import scriptella.spi.ProviderException;

import java.sql.SQLException;
import java.util.List;

/**
 * Unchecked wrapper for SQL exceptions or other SQL related errors.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JdbcException extends ProviderException {

    public JdbcException(String message) {
        super(message);
    }

    public JdbcException(String message, Throwable cause) {
        super(message, cause);
        initVendorCodes(cause);
    }

    public JdbcException(String message, Throwable cause, String sql, List<?> parameters) {
        super(message, cause);
        initVendorCodes(cause);
        setErrorStatement(sql, parameters);
    }

    public JdbcException(String message, Throwable cause, String sql) {
        super(message, cause);
        initVendorCodes(cause);
        setErrorStatement(sql, null);
    }
    public JdbcException(String message, String sql) {
        super(message);
        setErrorStatement(sql, null);
    }


    ProviderException setErrorStatement(String sql, List<?> params) {
        return super.setErrorStatement(sql + ((params == null || params.isEmpty()) ? "" : (". Parameters: " + params)));
    }


    protected void initVendorCodes(Throwable t) {
        if (t != null && t instanceof SQLException) {
            SQLException sqlEx = (SQLException) t;
            addErrorCode(sqlEx.getSQLState()).addErrorCode(String.valueOf(sqlEx.getErrorCode()));
        }

    }

    public Throwable getNativeException() {
        //Search for SQLException, which is important for user to see.
        for (Throwable e = getCause(); e != null; e = e.getCause()) {
            if (e instanceof SQLException) {
                return e;
            }
        }
        return null;
    }

    public String getProviderName() {
        return "JDBC";
    }

}
