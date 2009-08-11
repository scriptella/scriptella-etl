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
package scriptella.driver.auto;

import scriptella.AbstractTestCase;
import scriptella.configuration.ConfigurationException;
import scriptella.jdbc.GenericDriver;
import scriptella.spi.Connection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.MockConnectionParameters;

/**
 * Tests for {@link scriptella.driver.auto.Driver}.
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class AutoDriverTest extends AbstractTestCase {
    private String expectedDriver;
    public void test() {
        Driver d = new Driver() {
            @Override
            protected Connection getConnection(String driver, ConnectionParameters connectionParameters) {
                assertEquals(expectedDriver, driver);
                expectedDriver=null;
                return null;
            }
        };

        //Check several drivers which should be automatically discovered
        expectedDriver="h2";
        d.connect(new MockConnectionParameters(null, "jdbc:h2:....."));
        assertNull(expectedDriver);
        expectedDriver="jndi";
        d.connect(new MockConnectionParameters(null, "jndi:DataSource"));
        assertNull(expectedDriver);
        //Now check JDBC 4.0 auto-loading for unsupported JDBC driver
        expectedDriver= GenericDriver.class.getName(); //
        d.connect(new MockConnectionParameters(null, "jdbc:nosuchdb:object"));
        //Now check unsupported drivers
        try {
            d.connect(new MockConnectionParameters(null, "nosuchdriver:object"));
            fail("An error expected for unknown driver");
        } catch (ConfigurationException e) {
            //OK
        }

        //Now test for empty urls
        try {
            d.connect(new MockConnectionParameters(null, null));
            fail("Empty urls must be rejected");
        } catch (ConfigurationException e) {
            //OK
        }

    }
}
