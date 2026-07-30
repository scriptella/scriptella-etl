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
package scriptella.core;

import scriptella.AbstractTestCase;
import scriptella.jdbc.GenericDriver;
import scriptella.spi.ScriptellaDriver;

/**
 * Tests for {@link scriptella.core.DriverFactory}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DriverFactoryTest extends AbstractTestCase {

    /**
     * Tests correct handling of JDBC drivers on the test classpath (e.g. lib directory).
     * <p>Historical coverage of {@code sun.jdbc.odbc.JdbcOdbcDriver} was removed with
     * Chunk 6A — the JDK JDBC-ODBC bridge is not available on modern JDKs.
     */
    public void testClassPathDriver() throws ClassNotFoundException {
        ScriptellaDriver driver = DriverFactory.getDriver(
                "org.h2.Driver", getClass().getClassLoader());
        assertNotNull(driver);
        assertTrue(driver instanceof GenericDriver);
    }

    /**
     * Full class names of {@link scriptella.spi.ScriptellaDriver} implementations load directly.
     */
    public void testScriptellaDriverClassName() throws ClassNotFoundException {
        ScriptellaDriver driver = DriverFactory.getDriver(
                GenericDriver.class.getName(), getClass().getClassLoader());
        assertNotNull(driver);
        assertTrue(driver instanceof GenericDriver);
    }
}
