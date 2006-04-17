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

import scriptella.configuration.ConfigurationException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Substitutes properties(or expressions) in strings.
 * <p>In general $, ? or : symbols indicate  property or expression to evaluate and substitute.
 * <p>The following properties/expression syntax is used:
 * <h3>Property reference</h3>
 * References named property.
 * <br>Examples:
 * <pre><code>
 * $foo
 * ?bar or :name.
 * </code></pre>
 * <h3>Expression</h3>.
 * Expression is wrapped by braces and evaluated by {@link Expression} engine.
 * Examples:
 * <pre><code>
 * ${name+' '+surname}, :{this.file('index.html')} etc.
 * </code></pre>
 * </ul>
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class PropertiesSubstitutor {
    /**
     * Valid variable name
     */
    public static final String VAR = "([a-zA-Z_0-9\\.]+)";

    /**
     * Expression prefix to find properties/parameters/functions
     */
    public static final String EXPR_PREFIX = "[\\$\\?\\:]";

    /**
     * Simple property patterns, e.g. $property, ?property or :property
     */
    public static final Pattern PROP_PTR = Pattern.compile(EXPR_PREFIX + VAR);

    /**
     * Expression pattern, e.g. ${file 'url'} or ${property} etc.
     */
    public static final Pattern EXPR_PTR = Pattern.compile(EXPR_PREFIX +
            "\\{([^\\}]+)\\}");

    public PropertiesSubstitutor() {
    }

    /**
     * Substitutes properties/expressions in s and returns the result string.
     * <p>If result of evaluation is null or the property being substitued doesn't have value in callback - the whole
     * expressions is copied into result string as is.
     *
     * @param s        string to substitute.
     * @param callback callback to obtain paramter values.
     * @return substituted string.
     */
    public String substitute(final String s, final ParametersCallback callback) {
        if (s == null) {
            return null;
        }

        final int len = s.length();
        StringBuilder res = null;
        final Matcher m1 = PROP_PTR.matcher(s);
        final Matcher m2 = EXPR_PTR.matcher(s);
        int pos = 0;

        for (Matcher m = null; pos < len;) {
            int start = -1;

            if (m1.find(pos)) {
                m = m1;
                start = m1.start();
            }

            if (m2.find(pos)) {
                if ((start < 0) || (m2.start() < start)) {
                    m = m2;
                    start = m2.start();
                }
            }

            if (start < 0) {
                break;
            }

            if (res == null) {
                res = new StringBuilder(len);
            }

            if ((start - pos) > 0) { //Performance optimization
                res.append(s.substring(pos, m.start()));
            }

            final String pr = m.group();
            final String name = m.group(1);
            String v = null;

            if (m == m1) {
                v = substituteProperty(name, callback);
            } else {
                final Object o = substituteExpression(name, callback);

                if (o != null) {
                    v = o.toString();
                }
            }

            if (v != null) {
                res.append(v);
            } else {
                res.append(pr);
            }

            pos = m.end();
        }

        if (pos == 0) { //no matches

            return s;
        }

        if (pos < len) { //appends tail
            res.append(s.substring(pos, len));
        }

        return res.toString();
    }

    protected String substituteProperty(final String p,
                                        final ParametersCallback callback) {
        Object par = callback.getParameter(p);

        return (par == null) ? null : String.valueOf(par);
    }

    protected Object substituteExpression(final String expression,
                                          final ParametersCallback callback) {
        try {
            final Object o = Expression.compile(expression).evaluate(callback);

            return o;
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    public static void main(final String args[]) {
        final Map<String, String> m = new HashMap<String, String>();
        m.put("p1", "Prop1");
        m.put("p2", "Prop2");

        PropertiesSubstitutor ps = new PropertiesSubstitutor() {
            protected String substituteProperty(final String p) {
                return m.get(p);
            }

            protected Object substituteExpression(final String expression,
                                                  final ParametersCallback callback) {
                return null;
            }
        };

        final String s = ps.substitute("Just a $p1 test and $p2 . and $p3", null);
        long ti = System.currentTimeMillis();

        for (int i = 0; i < 100000; i++) {
            ps.substitute("Just a $p1 test and $p2. and $p3", null);
        }

        ti = System.currentTimeMillis() - ti;
        System.out.println("ti = " + ti);
        System.out.println("s = " + s);
    }
}
