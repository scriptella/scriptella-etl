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
package scriptella.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * A dynamic proxy adapter which allows overriding several
 * methods of a target proxy.
 * <p>To create a proxy adapter for Interface, create subclass of
 * ProxyAdapter and define methods from Interface you want to handle,
 * other methods invocations will be failed.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ProxyAdapter<T> {
    private final T proxy;

    @SuppressWarnings("unchecked")
    public ProxyAdapter(Class... interfaces) {
        proxy = (T) Proxy.newProxyInstance(getClass().getClassLoader(), interfaces, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Method m;
                try {
                    //Determine if the method has been defined in a subclass
                    m = ProxyAdapter.this.getClass().getMethod(method.getName(), method.getParameterTypes());
                    m.setAccessible(true);
                } catch (Exception e) { //if not found
                    throw new UnsupportedOperationException(method.toString(), e);

                }
                //Invoke the method found and return the result
                try {
                    return m.invoke(ProxyAdapter.this, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        });
    }

    /**
     * @return proxy instance implementing T.
     */
    public T getProxy() {
        return proxy;
    }


    /**
     * Usage example
     */
    public static void main(String[] args) throws Exception {
        //Create adapter for Appendable
        ProxyAdapter<Appendable> pa = new ProxyAdapter(Appendable.class) {
            private StringBuilder sb = new StringBuilder();
            //Override only 2 methods: append and toString
            public Appendable append(char c) {
                System.out.println("Proxy append(char c) method. Append "+c);
                sb.append(c);
                return (Appendable) getProxy();
            }

            public String toString() {
                return "Proxy toString method: "+sb;
            }

        };
        final Appendable a = pa.getProxy();
        a.append('1').append('2');
        System.out.println("a.toString() = " + a.toString());
        //this invocation fails because no method has been created
        a.append("Not implemented");

    }
}
