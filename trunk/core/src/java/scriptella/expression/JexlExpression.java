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
package scriptella.expression;

import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.parser.TokenMgrError;
import scriptella.core.EtlVariable;
import scriptella.spi.ParametersCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents <a href="jakarta.apache.org/commons/jexl">JEXL</a> expression.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public final class JexlExpression extends Expression {
    private org.apache.commons.jexl2.Expression expression;
    private static final JexlEngine jexlEngine = newJexlEngine();

    protected JexlExpression(String expression) throws ParseException {
        super(expression);

        try {
            this.expression = jexlEngine.createExpression(expression);
        } catch (Exception e) {
            throw new ParseException(e.getMessage(), e);
        } catch (TokenMgrError e) {
            throw new ParseException(e.getMessage(), e);
        }
    }

    public Object evaluate(final ParametersCallback callback)
            throws EvaluationException {
        JexlContextAdapter a = new JexlContextAdapter(callback);
        try {
            return expression.evaluate(a);
        } catch (Exception e) {
            throw new EvaluationException(e);
        }

    }

    /**
     * Creates a preconfigured JexlEngine.
     * <p>The instance is configured to use function namespaces from {@link scriptella.core.EtlVariable}.
     *
     * @return instance of JexlEngine.
     */
    public static JexlEngine newJexlEngine() {
        JexlEngine je = new JexlEngine();
        Map<String, Object> fMap = new HashMap<String, Object>();
        EtlVariable etl = new EtlVariable();
        fMap.put("date", etl.getDate());
        fMap.put("text", etl.getText());
        fMap.put("class", etl.getClazz());
        je.setFunctions(fMap);
        return je;
    }

    /**
     * Adapter for JexlContext to allow working with {@link ParametersCallback}.
     * <p>Also implements Map to represent ParametersCallback.
     */
    private static class JexlContextAdapter implements JexlContext {
        private ParametersCallback callback;

        private JexlContextAdapter(ParametersCallback callback) {
            this.callback = callback;
        }

        public Object get(final String name) {
            return callback.getParameter(name);
        }

        public void set(String s, Object o) {
            throw new UnsupportedOperationException("Setting variables in ${} JEXL expression is not allowed");
        }

        public boolean has(String name) {
            //Current model does not allow to distinguish between null value and absence, so we assume
            //variable is always present, otherwise JEXL will log warnings and throws errors internally
            return true;
        }
    }

}
