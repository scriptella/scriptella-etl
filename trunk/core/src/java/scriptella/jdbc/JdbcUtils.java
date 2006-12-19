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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * Utility class JDBC related operations.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public final class JdbcUtils {
    private JdbcUtils() {
    }

    /**
     * Silently closes a connection.
     * @param con connection to close. Nulls allowed.
     */
    public static void closeSilent(final Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        } catch (SQLException e) {
        }
    }

    /**
     * Silently closes a statement.
     * @param s statement to close. Nulls allowed.
     */
    public static void closeSilent(final Statement s) {
        try {
            if (s != null) {
                s.close();
            }
        } catch (SQLException e) {
        }
    }

    /**
     * Silently closes a result set.
     * @param rs result set to close. Nulls allowed.
     */
    public static void closeSilent(final ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
        }
    }
    
}
