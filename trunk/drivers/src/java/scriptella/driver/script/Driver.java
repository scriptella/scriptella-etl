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
package scriptella.driver.script;

import scriptella.spi.AbstractScriptellaDriver;
import scriptella.spi.Connection;
import scriptella.spi.ConnectionParameters;

/**
 * Scriptella bridge for the JSR 223: Scripting for the Java Platform.
 * <p/>
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class Driver extends AbstractScriptellaDriver {

    public Driver() {
        try { //Check if javax.script is available
            Class.forName("javax.script.ScriptEngine");
        } catch (ClassNotFoundException e) {
            throw new ScriptProviderException("Java SE 6 or higher is required for this driver to operate.", e);
        }
    }

    public Connection connect(ConnectionParameters connectionParameters) {
        return new ScriptConnection(connectionParameters);
    }
}
