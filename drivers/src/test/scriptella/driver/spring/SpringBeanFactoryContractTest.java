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
package scriptella.driver.spring;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import scriptella.AbstractTestCase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.util.Properties;

/**
 * Characterization of the Spring BeanFactory thread-local contract used by
 * {@code spring:} JDBC lookups during ETL execution (bug #4648).
 * <p>A Spring 5.x migration must preserve this association model even if
 * {@code SingletonBeanFactoryLocator} is replaced.
 *
 * @author Scriptella Project Team
 */
public class SpringBeanFactoryContractTest extends AbstractTestCase {

    public void testContextBeanFactoryAssociation() throws Exception {
        Method setContext = EtlExecutorBean.class.getDeclaredMethod(
                "setContextBeanFactory", BeanFactory.class);
        setContext.setAccessible(true);

        StaticApplicationContext ctx = new StaticApplicationContext();
        ctx.refresh();

        setContext.invoke(null, ctx);
        try {
            assertSame(ctx, EtlExecutorBean.getContextBeanFactory());
        } finally {
            setContext.invoke(null, new Object[]{null});
        }

        try {
            EtlExecutorBean.getContextBeanFactory();
            fail("BeanFactory association must be clearable");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    public void testMissingContextBeanFactoryFailsClearly() {
        try {
            EtlExecutorBean.getContextBeanFactory();
            fail("Expected IllegalStateException without an associated factory");
        } catch (IllegalStateException e) {
            String msg = e.getMessage().toLowerCase();
            assertTrue(msg.contains("beanfactory") || msg.contains("thread"));
        }
    }

    public void testDriverRequiresSpringOnClasspath() {
        // Smoke: constructing the driver resolves BeanFactory.
        new Driver();
    }

    public void testChildLoadedDriverUsesParentBeanFactory() throws Exception {
        Class<?> loaderType = Class.forName("scriptella.core.DriverClassLoader");
        Constructor<?> constructor = loaderType.getDeclaredConstructor(URL[].class);
        constructor.setAccessible(true);
        ClassLoader childLoader = (ClassLoader) constructor.newInstance(
                new Object[]{new URL[]{new URL("file:child-loader-regression")}});

        Class<?> childDriverType = Class.forName(Driver.class.getName(), true, childLoader);
        assertNotSame(Driver.class, childDriverType);
        assertNotSame(EtlExecutorBean.class,
                Class.forName(EtlExecutorBean.class.getName(), true, childLoader));

        StaticApplicationContext ctx = new StaticApplicationContext();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:springChildLoader;DB_CLOSE_DELAY=-1", "sa", "");
        ctx.getBeanFactory().registerSingleton("datasource", dataSource);
        ctx.refresh();

        Method setContext = EtlExecutorBean.class.getDeclaredMethod(
                "setContextBeanFactory", BeanFactory.class);
        setContext.setAccessible(true);
        setContext.invoke(null, ctx);
        try {
            Object childDriver = childDriverType.getDeclaredConstructor().newInstance();
            Method getConnection = childDriverType.getDeclaredMethod(
                    "getConnection", String.class, Properties.class);
            getConnection.setAccessible(true);
            Connection connection = (Connection) getConnection.invoke(
                    childDriver, "datasource", new Properties());
            try {
                assertFalse(connection.isClosed());
            } finally {
                connection.close();
            }
        } finally {
            setContext.invoke(null, new Object[]{null});
            ctx.close();
        }
        assertNoContextBeanFactory();
    }

    static void assertNoContextBeanFactory() {
        try {
            EtlExecutorBean.getContextBeanFactory();
            fail("BeanFactory association must be cleared after execution");
        } catch (IllegalStateException expected) {
            // ok
        }
    }
}
