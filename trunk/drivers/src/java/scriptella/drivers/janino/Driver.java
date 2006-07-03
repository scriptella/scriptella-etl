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
package scriptella.drivers.janino;

import scriptella.spi.AbstractScriptellaDriver;
import scriptella.spi.Connection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.DialectIdentifier;

/**
 * Scriptella Driver for <a href="http://www.janino.net/">Janino</a> Java Compiler.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class Driver extends AbstractScriptellaDriver {
    static final DialectIdentifier DIALECT_IDENTIFIER = new DialectIdentifier("Janino", "2.45"); 

    public Connection connect(ConnectionParameters connectionParameters) {
        return new JaninoConnection();
    }

}
