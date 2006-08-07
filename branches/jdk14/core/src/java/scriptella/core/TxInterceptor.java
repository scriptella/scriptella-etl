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

import scriptella.configuration.ScriptEl;
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

    public TxInterceptor(ExecutableElement next, ScriptEl scriptEl) {
        super(next, new TxDecorator(scriptEl));
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
                    "Script " + ctxDecorator.scriptEl.getLocation() +
                            " failed during invocation in a separate transaction", e);
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
        private ScriptEl scriptEl;
        private Connection c;

        public TxDecorator(ScriptEl scriptEl) {
            this.scriptEl = scriptEl;
        }

        @Override
        public Connection getConnection() {
            if (c == null) {
                c = getNewConnection();
            }

            return c;
        }
    }
}
