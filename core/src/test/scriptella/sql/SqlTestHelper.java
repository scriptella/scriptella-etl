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

import java.sql.Connection;
import java.util.List;
import java.util.Map;


/**
 * Utility class provides accessors for SQLEngine internal state.
 */
public class SqlTestHelper {
    public static Map<String, ConnectionFactory> getConnections(
            final SQLEngine engine) {
        return engine.connections;
    }

    public static Connection getConnection(final ConnectionFactory cf) {
        return cf.connection;
    }

    public static List<Connection> getNewConnections(final ConnectionFactory cf) {
        return cf.newConnections;
    }
}
