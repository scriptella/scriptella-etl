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
package scriptella.expressions;

import scriptella.execution.SystemException;

import java.util.Map;
import java.util.WeakHashMap;


/**
 * Base class for all expressions.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class Expression {
    private static final Map<String, Expression> EXPRESSIONS_CACHE = new WeakHashMap<String, Expression>();
    private String expression;

    protected Expression(String expression) {
        this.expression = expression;
    }

    public abstract Object evaluate(final ParametersCallback callback)
            throws EvaluationException;

    public String getExpression() {
        return expression;
    }

    public static Expression compile(final String expression)
            throws ParseException {
        Expression ex = EXPRESSIONS_CACHE.get(expression);

        if (ex != null) {
            return ex;
        }

        ex = new JexlExpression(expression);
        EXPRESSIONS_CACHE.put(expression, ex);

        return ex;
    }

    public static class ParseException extends SystemException {
        public ParseException() {
        }

        public ParseException(String message) {
            super(message);
        }

        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }

        public ParseException(Throwable cause) {
            super(cause);
        }
    }

    public static class EvaluationException extends SystemException {
        public EvaluationException() {
        }

        public EvaluationException(String message) {
            super(message);
        }

        public EvaluationException(String message, Throwable cause) {
            super(message, cause);
        }

        public EvaluationException(Throwable cause) {
            super(cause);
        }
    }
}
