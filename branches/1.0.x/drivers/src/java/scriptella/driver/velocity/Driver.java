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
package scriptella.driver.velocity;

import scriptella.spi.AbstractScriptellaDriver;
import scriptella.spi.Connection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.DialectIdentifier;

import java.util.logging.Logger;

/**
 * Scriptella Driver for <a href="http://jakarta.apache.org/velocity">Velocity</a> template engine.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class Driver extends AbstractScriptellaDriver {
    static final DialectIdentifier DIALECT = new DialectIdentifier("Velocity", "1.4");
    static final Logger LOG = Logger.getLogger(Driver.class.getName());


    public Driver() {
        try { //Check if velocity is on classpath
            Class.forName("org.apache.velocity.VelocityContext");
        } catch (ClassNotFoundException e) {
            throw new VelocityProviderException("Velocity not found on classpath. Check if connection classpath attribute points to velocity-dep.jar");
        }
    }

    /**
     * Implementor should create a new connection based on specified parameters.
     *
     * @param connectionParameters connection parameters defined in &lt;connection&gt; element.
     * @return new connection.
     */
    public Connection connect(ConnectionParameters connectionParameters) {
        return new VelocityConnection(connectionParameters);
    }


}
