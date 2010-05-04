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
package scriptella.core;

import scriptella.configuration.Location;
import scriptella.configuration.ScriptEl;
import scriptella.configuration.ScriptingElement;
import scriptella.spi.Connection;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class TxInterceptor extends ElementInterceptor {
    private static final Logger LOG = Logger.getLogger(TxInterceptor.class.getName());
    private Location location;

    public TxInterceptor(ExecutableElement next, ScriptEl scriptEl) {
        super(next, new TxDecorator(scriptEl));
        location=scriptEl.getLocation();
    }

    public void execute(final DynamicContext ctx) {
        final TxDecorator ctxDecorator = (TxDecorator) getCtxDecorator();
        ctxDecorator.setContext(ctx);

        try {
            executeNext(ctxDecorator);
        } catch (Throwable e) {
            try {
                ctxDecorator.c.rollback();
            } catch (Exception e1) {
                LOG.log(Level.WARNING, "Unable to rollback transaction", e1);
            }
            LOG.log(Level.INFO,
                    "Script " + location + " failed during invocation in a separate transaction", e);
        }
    }

    public static ExecutableElement prepare(
            final ExecutableElement next, final ScriptEl s) {
        if (s.isNewTx()) {
            return new TxInterceptor(next, s);

        } else {
            return next;
        }
    }

    private static class TxDecorator extends DynamicContextDecorator {
        private Connection c;
        private String connectionId;

        public TxDecorator(ScriptEl script) {
            String cid = script.getConnectionId();
            //if connection id is null, iterate parents to get connection
            if (cid == null) {
                for (ScriptingElement s = script; (s = s.getParent()) != null;) {
                    cid = s.getConnectionId();
                    if (cid != null) {
                        break;
                    }
                }
            }
            connectionId=cid; //If single connection script
        }

        @Override
        public Connection getConnection() {
            if (c==null) {
                c=getGlobalContext().getSession().getConnection(connectionId).newConnection();
            }
            return c;
        }
    }
}
