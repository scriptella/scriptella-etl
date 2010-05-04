/*
 * Copyright 2006-2009 The Scriptella Project Team.
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

import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.QueryCallback;
import scriptella.util.IOUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Query abstraction to for tests.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class QueryHelper {
    private String sql;

    public QueryHelper(String sql) {
        this.sql = sql;
    }

    /**
     * Executes a query for connection.
     * @param con connection to use.
     * @param callback callback to call for rows being iterated.
     */
    public void execute(Connection con, QueryCallback callback) {
        Statement st = null;
        ResultSetAdapter ra = null;
        try {
            st=con.createStatement();
            ra = new ResultSetAdapter(st.executeQuery(sql),
                    MockParametersCallbacks.UNSUPPORTED, new JdbcTypesConverter());
            while (ra.next()) {
                callback.processRow(ra);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            IOUtils.closeSilently(ra);
            JdbcUtils.closeSilent(st);
        }

    }

    protected void onSQLException(SQLException e) {
        throw new IllegalStateException(e.getMessage(), e);
    }
}
