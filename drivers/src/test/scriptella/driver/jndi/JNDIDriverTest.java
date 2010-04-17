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
package scriptella.driver.jndi;

import scriptella.AbstractTestCase;
import scriptella.configuration.MockConnectionEl;
import scriptella.jdbc.JdbcConnection;
import scriptella.jdbc.JdbcException;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.MockDriverContext;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Tests JNDI driver class.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JNDIDriverTest extends AbstractTestCase {

    /**
     * Tests the driver by emulating the JNDI environment with a JNDI-bound datasource.
     */
    public void testGetConnection() throws NamingException {
        //Just to initialize HSLQB driver class
        Logger.getAnonymousLogger().fine("Initializing " + new scriptella.driver.hsqldb.Driver());
        //Preparing the environment
        Map<String, String> env = new HashMap<String, String>();
        //Setting up a test JNDI factory
        env.put(Context.INITIAL_CONTEXT_FACTORY, CtxFactory.class.getName());
        CtxFactory.jndiName = "datasourceName";
        CtxFactory.connections = 0;
        CtxFactory.lookups = 0;
        ConnectionParameters params = new ConnectionParameters(new MockConnectionEl(env, CtxFactory.jndiName), MockDriverContext.INSTANCE);
        Driver drv = new Driver();
        JdbcConnection con1 = drv.connect(params);
        con1.close();
        assertNotNull(con1);
        JdbcConnection con2 = drv.connect(params);
        con2.close();
        assertNotNull(con2);
        assertTrue("con1 and con2 must be different connections", con1 != con2);
        //lookup and getConnection called 2 times
        assertEquals("Illegal number of lookups", 2, CtxFactory.lookups);
        assertEquals("Illegal number of created connections", 2, CtxFactory.connections);

    }

    /**
     * Tests if validation of connection parameters is performed.
     * @throws SQLException
     */
    public void testValidation() throws SQLException {
        Driver drv = new Driver();
        try {
            drv.getConnection(null , null);
        } catch (JdbcException e) {
            //ok
        }

    }

    /**
     * Represents {@link InitialContextFactory} for JNDI and an {@link InvocationHandler} for
     * emulating Context and Datasource simultaneously.
     */
    public static class CtxFactory implements InitialContextFactory, InvocationHandler {
        public static String jndiName;
        public static int lookups;
        public static int connections;

        public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
            return (Context) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Context.class, DataSource.class}, this);
        }

        /**
         * This invoker supports 2 methods:
         * {@link Context#lookup(String)} and {@link javax.sql.DataSource#getConnection()}
         *
         * @throws SQLException if db exception occurs
         */
        public Object invoke(Object proxy, Method method, Object[] args) throws SQLException {
            if ("lookup".equals(method.getName())) {
                lookups++;
                String name = (String) args[0];
                if (!jndiName.equals(name)) {
                    fail("Expected " + jndiName + " JNDI name but was " + name);
                }
                return proxy;
            } else if ("getConnection".equals(method.getName())) {
                connections++;
                return DriverManager.getConnection("jdbc:hsqldb:mem:jnditest", "sa", "");
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

}
