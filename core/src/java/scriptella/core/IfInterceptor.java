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

import scriptella.configuration.Location;
import scriptella.configuration.ScriptingElement;
import scriptella.expressions.Expression;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Handles if expressions specified by if attribute on query/script elements.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 * @see scriptella.expressions.JexlExpression
 */
public class IfInterceptor extends ElementInterceptor {
    private static final Logger LOG = Logger.getLogger(IfInterceptor.class.getName());
    private static final Set<CharSequence> trueStrs = new LinkedHashSet<CharSequence>();

    static {
        trueStrs.add("true");
        trueStrs.add("yes");
        trueStrs.add("1");
        trueStrs.add("on");
    }

    private Expression expression;
    private Location location;

    public IfInterceptor(ExecutableElement next, ScriptingElement scr) {
        super(next);
        expression = Expression.compile(scr.getIf());
        location = scr.getLocation();
    }

    public void execute(final DynamicContext ctx) {
        boolean ok = false;

        try {
            final Object res = expression.evaluate(ctx);

            if (res != null) {
                if (res instanceof Boolean) {
                    ok = (Boolean) res;
                } else if (trueStrs.contains(String.valueOf(res))) {
                    ok = true;
                }
            }
        } catch (Expression.EvaluationException e) {
            LOG.log(Level.WARNING,
                    "Unable to evaluate if condition \"" +
                            expression.getExpression() + "\" for script " + location +
                            ": " + e.getMessage(), e);
        }

        if (ok) { //if expr evaluated to true
            executeNext(ctx);
        }
    }

    public static ExecutableElement prepare(
            final ExecutableElement next, final ScriptingElement s) {
        final String ifExpr = s.getIf();

        if ((ifExpr == null) || (ifExpr.length() == 0)) {
            return next;
        }

        return new IfInterceptor(next, s);
    }
}
