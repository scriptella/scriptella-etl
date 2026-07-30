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
    private static final String[][] DEPENDENCIES = {
            {"org.apache.velocity.VelocityContext", "velocity.jar"},
            {"org.apache.commons.collections.ExtendedProperties", "commons-collections.jar"},
            {"org.apache.commons.lang.StringUtils", "commons-lang.jar"}
    };


    public Driver() {
        checkDependencies(getClass().getClassLoader());
    }

    static void checkDependencies(ClassLoader classLoader) {
        for (String[] dependency : DEPENDENCIES) {
            try {
                Class.forName(dependency[0], false, classLoader);
            } catch (ClassNotFoundException e) {
                throw missingDependency(dependency[1], e);
            } catch (LinkageError e) {
                throw missingDependency(dependency[1], e);
            }
        }
    }

    private static VelocityProviderException missingDependency(String jar, Throwable cause) {
        return new VelocityProviderException("Unable to load the Velocity dependency " + jar
                + ". Check if the connection classpath attribute points to velocity.jar, "
                + "commons-collections.jar, and commons-lang.jar", cause);
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
