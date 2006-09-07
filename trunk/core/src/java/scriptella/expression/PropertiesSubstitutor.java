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
package scriptella.expression;

import scriptella.spi.ParametersCallback;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Substitutes properties(or expressions) in strings.
 * <p>$ symbol indicate  property or expression to evaluate and substitute.
 * <p>The following properties/expression syntax is used:
 * <h3>Property reference</h3>
 * References named property.
 * <br>Examples:
 * <pre><code>
 * $foo
 * </code></pre>
 * <h3>Expression</h3>.
 * Expression is wrapped by braces and evaluated by {@link Expression} engine.
 * Examples:
 * <pre><code>
 * ${name+' '+surname} etc.
 * </code></pre>
 * </ul>
 * <p>This class is not thread safe
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class PropertiesSubstitutor {
    /**
     * Simple property patterns, e.g. $property
     */
    public static final Pattern PROP_PTR = Pattern.compile("([a-zA-Z_0-9\\.]+)");

    /**
     * Expression pattern, e.g. ${property} etc.
     */
    public static final Pattern EXPR_PTR = Pattern.compile("\\{([^\\}]+)\\}");

    final Matcher m1 = PROP_PTR.matcher("");
    final Matcher m2 = EXPR_PTR.matcher("");

    /**
     * Creates a properties substitutor.
     * <p>This constructor is used for performance critical places where multiple instantiation
     * via {@link #PropertiesSubstitutor(scriptella.spi.ParametersCallback)} is expensive.
     * <p><b>Note:</b> {@link #setParameters(scriptella.spi.ParametersCallback)} must be called before
     * {@link #substitute(String)}.
     */
    public PropertiesSubstitutor() {
    }

    /**
     * Creates a properties substitutor.
     * @param parameters parameters callback to use for substitution.
     */
    public PropertiesSubstitutor(ParametersCallback parameters) {
        this.parameters = parameters;
    }

    private ParametersCallback parameters;

    /**
     * Substitutes properties/expressions in s and returns the result string.
     * <p>If result of evaluation is null or the property being substitued doesn't have value in callback - the whole
     * expressions is copied into result string as is.
     *
     * @param s string to substitute. Null strings allowed.
     * @return substituted string.
     */
    public String substitute(final String s) {
        if (s == null) {
            return null;
        }
        if (parameters==null) {
            throw new IllegalStateException("setParameters must be called before calling substitute");
        }

        final int len = s.length()-1; //Last character is not checked - optimization
        if (len <= 0) { //skip empty strings or single characters
            return s;
        }
        StringBuilder res = null;
        char[] sChars = s.toCharArray();
        int lastPos=0;
        m1.reset(s);
        m2.reset(s);
        for (int i = 0; i < len; i++) {
            char c = sChars[i];
            switch (c) {
                case '$':
                    //Start of expression
                    Matcher m;
                    if (m1.find(i+1) && m1.start()==i+1) {
                        m=m1;
                    } else if (m2.find(i+1) && m2.start()==i+1) {
                        m = m2;
                    } else { //not an expression
                        m=null;
                    }
                    if (m!=null) {
                        if (res==null) {
                            res = new StringBuilder(s.length());
                        }
                        if (i>lastPos) { //if we have unflushed character
                            res.append(sChars, lastPos,i-lastPos);
                        }
                        final String name = m.group(1);
                        String v = null;

                        if (m == m1) {
                            v = toString(parameters.getParameter(name));
                        } else {
                            v = toString(Expression.compile(name).evaluate(parameters));
                        }

                        lastPos=m.end();
                        if (v != null) {
                            res.append(v);
                        } else { //appends the original string
                            res.append(sChars, i,lastPos-i);
                        }

                    }


                    break;
                default:


            }
        }
        if (res==null) {
            return s;
        }
        if (lastPos<=len) {
            res.append(sChars, lastPos, s.length()-lastPos);
        }


        return res.toString();
    }

    /**
     *
     * @return parameter callback used for substitution.
     */
    public ParametersCallback getParameters() {
        return parameters;
    }

    /**
     * Sets parameters callback used for substitution.
     * @param parameters not null parameters callback.
     */
    public void setParameters(ParametersCallback parameters) {
        this.parameters = parameters;
    }

    /**
     * Converts specified object to string.
     * <p>Subclasses may provide custom conversion strategy here.
     * @param o object to convert to String.
     * @return string representation of object.
     */
    protected String toString(final Object o) {
        return o==null?null:o.toString();
    }

}
