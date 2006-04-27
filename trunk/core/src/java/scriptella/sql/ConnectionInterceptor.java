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

import scriptella.configuration.SQLBasedElement;

import java.sql.Connection;


/**
 * Handles connections for elements being executed.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConnectionInterceptor extends SQLElementInterceptor {
    public ConnectionInterceptor(SQLExecutableElement next, SQLBasedElement s) {
        super(next, new ConnectionDecorator(s.getConnectionId()));
    }

    public void execute(final SQLContext ctx) {
        final SQLContextDecorator ctxDecorator = getCtxDecorator();
        ctxDecorator.setContext(ctx);
        executeNext(ctxDecorator);
    }

    public static SQLExecutableElement prepare(
            final SQLExecutableElement next, final SQLBasedElement s) {
        if (s.getConnectionId() == null) {
            return next;
        } else {
            return new ConnectionInterceptor(next, s);
        }
    }

    private static class ConnectionDecorator extends SQLContextDecorator {
        private String connectionId;

        public ConnectionDecorator(String connectionId) {
            this.connectionId = connectionId;
        }

        @Override
        public Connection getConnection() {
            return getGlobalContext().getSqlEngine().getConnection(connectionId)
                    .getConnection();
        }

        @Override
        public Connection getNewConnection() {
            return getGlobalContext().getSqlEngine().getConnection(connectionId)
                    .newConnection();
        }

        @Override
        public DialectIdentifier getDialectIdentifier() {
            return getGlobalContext().getSqlEngine().getConnection(connectionId)
                    .getDialectIdentifier();
        }
    }
}
