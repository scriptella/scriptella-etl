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
package scriptella;

import junit.framework.AssertionFailedError;
import scriptella.jdbc.JdbcException;
import scriptella.jdbc.JdbcUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class DBTestCase extends AbstractTestCase {
    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new AssertionFailedError(e.getMessage());
        }
    }

    private Collection<Connection> connections = new ArrayList<Connection>();

    protected Connection getConnection(final String db) {
        try {
            final Connection c = DriverManager.getConnection("jdbc:h2:mem:" +
                    db + ";MODE=LEGACY;NON_KEYWORDS=VALUE", "sa", "");
            connections.add(c);

            return c;
        } catch (SQLException e) {
            throw new JdbcException(e.getMessage(), e);
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        for (Connection connection : connections) {
            connection.createStatement().execute("DROP ALL OBJECTS");
            JdbcUtils.closeSilent(connection);
        }

        connections.clear();
    }
}
