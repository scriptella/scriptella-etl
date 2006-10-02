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
package scriptella.spi;

import scriptella.AbstractTestCase;
import scriptella.jdbc.GenericDriver;

import java.net.URL;

/**
 * Tests {@link DriverClassLoader}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DriversClassLoaderTest extends AbstractTestCase {
    /**
     * Tests classloader delegation model.
     */
    public void testDelegation() throws ClassNotFoundException {
        Class bootClass = ScriptellaDriver.class;
        Class jdbcClass = GenericDriver.class;
        DriverClassLoader loader = new DriverClassLoader(new URL[0]);
        Class newClass = Class.forName(bootClass.getName(), false, loader);
        //boot classes show be the same
        assertEquals(bootClass, newClass);
        Class newJavaClass = Class.forName(String.class.getName(), false, loader);
        assertEquals(String.class, newJavaClass);
        //Now let's test reloadable classes
        Class newJDBCClass = Class.forName(jdbcClass.getName(), false, loader);
        assertNotSame(jdbcClass, newJDBCClass);
        //TODO Add additional classpath jars test
    }
}
