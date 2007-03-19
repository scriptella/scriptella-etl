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
package scriptella.expression;

import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.parser.TokenMgrError;
import scriptella.spi.ParametersCallback;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


/**
 * Represents <a href="jakarta.apache.org/commons/jexl">JEXL</a> expression.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JexlExpression extends Expression {
    private JexlContextAdapter adapter = new JexlContextAdapter();
    private org.apache.commons.jexl.Expression expression;

    protected JexlExpression(String expression) throws ParseException {
        super(expression);

        try {
            this.expression = ExpressionFactory.createExpression(expression);
        } catch (Exception e) {
            throw new ParseException(e.getMessage(),e);
        } catch (TokenMgrError e) {
            throw new ParseException(e.getMessage(),e);
        }
    }

    public Object evaluate(final ParametersCallback callback)
            throws EvaluationException {
        try {
            adapter.setContext(callback);

            return expression.evaluate(adapter);
        } catch (Exception e) {
            throw new EvaluationException(e);
        } finally {
            adapter.unsetContext();
        }
    }

    /**
     * Adapter for JexlContext to allow working with {@link ParametersCallback}.
     */
    private static class JexlContextAdapter implements JexlContext {
        private ParametersCallbackMap map = new ParametersCallbackMap();

        public void setVars(final Map map) {
        }

        public Map getVars() {
            return map;
        }

        public void setContext(final ParametersCallback parametersCallback) {
            map.callback = parametersCallback;
        }

        public void unsetContext() {
            map.callback = null;
        }
    }

    /**
     * Represents {@link ParametersCallback} as a Map.
     * <p>Only {@link #get(Object)} method is supported. Invocations of other methods result in
     * {@link UnsupportedOperationException} thrown.
     */
    private static final class ParametersCallbackMap implements Map {
        private ParametersCallback callback;

        public int size() {
            throw new UnsupportedOperationException();
        }

        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        public boolean containsKey(final Object key) {
            throw new UnsupportedOperationException();
        }

        public boolean containsValue(final Object value) {
            throw new UnsupportedOperationException();
        }

        public Object get(final Object key) {
            String name = (String) key;
            if (EtlVariable.NAME.equals(key)) {
                return EtlVariable.get();
            }
            return callback.getParameter(name);
        }

        public Object put(final Object key, final Object o1) {
            throw new UnsupportedOperationException();
        }

        public Object remove(final Object key) {
            throw new UnsupportedOperationException();
        }

        public void putAll(final Map map) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public Set keySet() {
            throw new UnsupportedOperationException();
        }

        public Collection values() {
            throw new UnsupportedOperationException();
        }

        public Set<Entry<String, Object>> entrySet() {
            throw new UnsupportedOperationException();
        }
    }
}
