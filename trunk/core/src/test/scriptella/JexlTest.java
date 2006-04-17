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
package scriptella;

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlExprResolver;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JexlTest {
    public static void main(final String args[]) throws Exception {
        final Expression ex = ExpressionFactory.createExpression("a.q==b");
        ex.addPreResolver(new JexlExprResolver() {
            public Object evaluate(final JexlContext jexlContext,
                                   final String s) {
                System.out.println("s = " + s);

                return NO_VALUE;
            }
        });

        JexlContext ctx = new JexlContext() {
            public void setVars(final Map map) {
            }

            public Map getVars() {
                return new Map() {
                    public int size() {
                        System.out.println("JexlTest.size");

                        return 0; //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public boolean isEmpty() {
                        System.out.println("JexlTest.isEmpty");

                        return false;
                    }

                    public boolean containsKey(final Object key) {
                        System.out.println("JexlTest.containsKey");

                        return false; //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public boolean containsValue(final Object value) {
                        System.out.println("JexlTest.containsValue");

                        return false; //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Object get(final Object key) {
                        System.out.println("JexlTest.get: " + key);

                        if (key.equals("b")) {
                            return null;
                        }

                        return "1"; //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Object put(final Object o, final Object o1) {
                        System.out.println("JexlTest.put");

                        return null; //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Object remove(final Object key) {
                        System.out.println("JexlTest.remove");

                        return null; //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void putAll(final Map map) {
                        System.out.println("JexlTest.putAll");

                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void clear() {
                        System.out.println("JexlTest.clear");

                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Set keySet() {
                        System.out.println("JexlTest.keySet");

                        return null; //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Collection values() {
                        System.out.println("JexlTest.values");

                        return null; //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Set<Entry> entrySet() {
                        System.out.println("JexlTest.entrySet");

                        return null; //To change body of implemented methods use File | Settings | File Templates.
                    }
                };
            }
        };

        final Object r = ex.evaluate(ctx);
        System.out.println("r = " + r);
        System.out.println("r.getClass() = " + r.getClass());
    }
}
