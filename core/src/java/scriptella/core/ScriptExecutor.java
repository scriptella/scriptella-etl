/*
 * Copyright 2006-2007 The Scriptella Project Team.
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

import scriptella.configuration.ContentEl;
import scriptella.configuration.OnErrorEl;
import scriptella.configuration.ScriptEl;
import scriptella.spi.Connection;
import scriptella.spi.DialectIdentifier;
import scriptella.spi.Resource;
import scriptella.util.ExceptionUtils;
import scriptella.util.StringUtils;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptExecutor extends ContentExecutor<ScriptEl> {
    private static final Logger LOG = Logger.getLogger(ScriptExecutor.class.getName());
    private final boolean debug=LOG.isLoggable(Level.FINE);

    public ScriptExecutor(ScriptEl scriptEl) {
        super(scriptEl);
    }

    public void execute(final DynamicContext ctx) {
        Connection con = ctx.getConnection();
        ScriptEl scriptEl = getElement();

        Resource content = getContent(con.getDialectIdentifier());
        if (content == ContentEl.NULL_CONTENT) {
            warnEmptyContent();
            return;
        }
        if (debug) {
            LOG.fine("Executing script " + getLocation());
        }
        boolean repeat;
        do {
            repeat = false;
            try {
                con.executeScript(content, ctx);
                if (debug) {
                    LOG.fine("Script " + getLocation() + " completed");
                }

            } catch (Throwable t) {
                if (scriptEl.getOnerrorElements() != null) {
                    repeat = onError(t, new OnErrorHandler(scriptEl), ctx);
                } else {
                    ExceptionUtils.throwUnchecked(t);
                }
            }
        } while (repeat); //repeat while onError returns retry
    }

    private void warnEmptyContent() {
        LOG.info("Script " + getLocation() + " has no supported dialects");
    }

    /**
     * Recursive on error fallback.
     *
     * @param t
     * @param errorHandler
     * @param ctx
     * @return true if script execution should be retried
     */
    private boolean onError(Throwable t, OnErrorHandler errorHandler, DynamicContext ctx) {
        OnErrorEl onErrorEl = errorHandler.onError(t);
        Connection con = ctx.getConnection();
        DialectIdentifier dialectId = con.getDialectIdentifier();
        if (onErrorEl != null) { //if error handler present for this case
            Resource content = onErrorEl.getContent(dialectId);
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, StringUtils.consoleFormat("Script " + getLocation() + " failed: " + t +
                        "\nUsing onError handler: " + onErrorEl));
            }

            try {
                con.executeScript(content, ctx);
                return onErrorEl.isRetry();
            } catch (Exception e) {
                return onError(e, errorHandler, ctx); //calling this method again and triying to find another onerror
            }
        } //if no onError found - rethrow the exception
        ExceptionUtils.throwUnchecked(t);
        return false;

    }

    public static ExecutableElement prepare(final ScriptEl s) {
        ExecutableElement se = new ScriptExecutor(s);
        se = StatisticInterceptor.prepare(se, s.getLocation());
        se = TxInterceptor.prepare(se, s);
        se = ConnectionInterceptor.prepare(se, s);
        se = ExceptionInterceptor.prepare(se, s.getLocation());
        se = IfInterceptor.prepare(se, s);

        return se;
    }
}
