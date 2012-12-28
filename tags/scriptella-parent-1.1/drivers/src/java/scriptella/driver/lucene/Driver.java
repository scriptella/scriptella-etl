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
package scriptella.driver.lucene;

import scriptella.spi.AbstractScriptellaDriver;
import scriptella.spi.Connection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.DialectIdentifier;


/**
 * Scriptella Driver for <a href="http://lucene.apache.org/">Lucene</a> Search Engine.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Kirill Volgin
 * @version 1.0
 */
public class Driver extends AbstractScriptellaDriver {
    static final DialectIdentifier DIALECT_IDENTIFIER = new DialectIdentifier("Lucene", "2.0.0");


    public Driver() {
        try {
            Class.forName("org.apache.lucene.store.Directory");
        } catch (ClassNotFoundException e) {
            throw new LuceneProviderException("Lucene not found on the class path. " +
                    "Check if connection classpath attribute points to lucene.jar", e);
        }
    }

    public Connection connect(ConnectionParameters connectionParameters) {
        return new LuceneConnection(connectionParameters);
    }

}
