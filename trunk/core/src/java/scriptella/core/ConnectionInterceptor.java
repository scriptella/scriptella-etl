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
package scriptella.core;

import scriptella.configuration.ScriptingElement;
import scriptella.spi.Connection;


/**
 * Handles connections for elements being executed.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConnectionInterceptor extends ElementInterceptor {
    private ConnectionInterceptor(ExecutableElement next, String conId) {
        super(next, new ConnectionDecorator(conId));
    }

    public void execute(final DynamicContext ctx) {
        final DynamicContextDecorator ctxDecorator = getCtxDecorator();
        ctxDecorator.setContext(ctx);
        executeNext(ctxDecorator);
    }

    public static ExecutableElement prepare(
            final ExecutableElement next, final ScriptingElement se) {
        final String cid = se.getConnectionId();
        if (cid == null) {
            return next;
        } else {
            for (ScriptingElement s = se; (s = s.getParent()) != null;) {
                if (s.getConnectionId() != null) {
                    if (cid.equals(s.getConnectionId())) {
                        return next;
                    }
                    break;
                }
            }
            return new ConnectionInterceptor(next, cid);
        }
    }

    private static class ConnectionDecorator extends DynamicContextDecorator {
        private String connectionId;

        public ConnectionDecorator(String connectionId) {
            this.connectionId = connectionId;
        }

        @Override
        public Connection getConnection() {
            return getGlobalContext().getSession().getConnection(connectionId)
                    .getConnection();
        }
    }
}
