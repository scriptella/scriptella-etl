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
package scriptella.core;

import scriptella.configuration.ContentEl;
import scriptella.configuration.OnErrorEl;
import scriptella.configuration.ScriptEl;
import scriptella.spi.Connection;
import scriptella.spi.DialectIdentifier;
import scriptella.spi.Resource;
import scriptella.spi.support.HierarchicalParametersCallback;
import scriptella.spi.support.MapParametersCallback;
import scriptella.util.ExceptionUtils;
import scriptella.util.StringUtils;

import java.util.Collections;
import java.util.logging.Level;


/**
 * &lt;script&gt; element executor.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public final class ScriptExecutor extends ContentExecutor<ScriptEl> {
    public ScriptExecutor(ScriptEl scriptEl) {
        super(scriptEl);
    }

    protected void execute(Connection connection, Resource resource, DynamicContext ctx) {
        if (debug) {
            log.fine("Executing script " + getLocation());
        }
        boolean repeat;
        OnErrorHandler onErrorHandler = null;
        do {
            repeat = false;
            try {
                connection.executeScript(resource, ctx);
                if (debug) {
                    log.fine("Script " + getLocation() + " completed");
                }
            } catch (Throwable t) {
                ScriptEl scriptEl = getElement();
                if (scriptEl.getOnerrorElements() != null) {
                    // Keep the same onerror handler during handling and retrying to avoid infinite loops
                    if (onErrorHandler == null) {
                        onErrorHandler = new OnErrorHandler(scriptEl);
                    }
                    repeat = onError(t, onErrorHandler, ctx);
                } else {
                    ExceptionUtils.throwUnchecked(t);
                }
            }
        } while (repeat); //repeat while onError returns retry
    }

    /**
     * Recursive on error fallback.
     *
     * @param t error to handle.
     * @param errorHandler error handler to use.
     * @param ctx dynamic context/
     * @return true if script execution should be retried
     */
    private boolean onError(Throwable t, OnErrorHandler errorHandler, DynamicContext ctx) {
        OnErrorEl onErrorEl = errorHandler.onError(t);
        if (onErrorEl != null) { //if error handler present for this case
            Connection con;
            //CRQ-54586 Feature request: Allow different connection-id in onerror element
            if (onErrorEl.getConnectionId() == null) {
                con = ctx.getConnection();
            } else {
                con = ctx.getGlobalContext().getSession().getConnection(onErrorEl.getConnectionId()).getConnection();
            }
            DialectIdentifier dialectId = con.getDialectIdentifier();

            ContentEl content = prepareContent(onErrorEl.getContent(dialectId));
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, StringUtils.consoleFormat("Script " + getLocation() + " failed: " + t +
                        "\nUsing onError handler: " + onErrorEl));
            }
            try {
                //Make error details available to the onerror handler
                //This is achieved by wrapping ctx into a new dynamic context with a variable "error"
                MapParametersCallback pc = new MapParametersCallback(Collections.singletonMap("error", t));
                HierarchicalParametersCallback hpc = new HierarchicalParametersCallback(pc, ctx);
                con.executeScript(content == null ? ContentEl.NULL_CONTENT : content, hpc);
                return onErrorEl.isRetry();
            } catch (Exception e) {
                return onError(e, errorHandler, ctx); //calling this method again and trying to find another onerror
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
