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
package scriptella;

import junit.framework.AssertionFailedError;
import scriptella.jdbc.JdbcException;
import scriptella.jdbc.JdbcUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class DBTestCase extends AbstractTestCase {
    static {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (ClassNotFoundException e) {
            throw new AssertionFailedError(e.getMessage());
        }
    }

    private Collection<String> dbNames = new HashSet<String>();
    private Collection<Connection> connections = new ArrayList<Connection>();

    protected Connection getConnection(final String db) {
        dbNames.add(db);

        try {
            final Connection c = DriverManager.getConnection("jdbc:hsqldb:mem:" +
                    db, "sa", "");
            connections.add(c);

            return c;
        } catch (SQLException e) {
            throw new JdbcException(e.getMessage(), e);
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        for (String s : dbNames) {
            try {
                getConnection(s).createStatement().execute("SHUTDOWN");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (Connection connection : connections) {
            JdbcUtils.closeSilent(connection);
        }

        dbNames.clear();
        connections.clear();
    }
}
