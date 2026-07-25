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
import scriptella.AbstractTestCase;

import java.lang.reflect.Method;

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
}
