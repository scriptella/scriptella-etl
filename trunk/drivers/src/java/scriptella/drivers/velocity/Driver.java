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
package scriptella.drivers.velocity;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;
import scriptella.spi.AbstractScriptellaDriver;
import scriptella.spi.Connection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.DialectIdentifier;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scriptella Driver for <a href="http://jakarta.apache.org/velocity">Velocity</a> template engine.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class Driver extends AbstractScriptellaDriver {
    static final DialectIdentifier DIALECT = new DialectIdentifier("Velocity", "1.4");
    public static final String OUTPUT_ENCODING = "output.encoding";
    private static final Logger LOG = Logger.getLogger(Driver.class.getName());
    static final LogSystem LOG_SYSTEM = new LogSystem() {
        public void init(RuntimeServices rs) {
        }

        public void logVelocityMessage(int level, String message) {
            if (level < 0) {
                return;
            }
            Level lev; //converting velocity level to JUL
            switch (level) {
                case DEBUG_ID:
                    lev = Level.FINE;
                    break;
                case INFO_ID: //Velocity INFO is too verbose
                    lev = Level.CONFIG;
                    break;
                case WARN_ID:
                    lev = Level.INFO;
                    break;
                case ERROR_ID:
                    lev = Level.WARNING;
                    break;
                default:
                    lev = Level.INFO;
            }
            if (LOG.isLoggable(lev)) {
                LOG.log(lev, "Engine: " + message);
            }
        }
    };

    /**
     * Implementor should create a new connection based on specified parameters.
     *
     * @param connectionParameters connection parameters defined in &lt;connection&gt; element.
     * @return new connection.
     */
    public Connection connect(ConnectionParameters connectionParameters) {
        //TODO: Add support for output file encoding
        String urlStr = connectionParameters.getUrl();
        String outEnc = connectionParameters.getProperty(OUTPUT_ENCODING);
        URL url;
        try {
            url = new URL(urlStr);
            return new VelocityConnection(url, outEnc);
        } catch (MalformedURLException e) {
            throw new VelocityProviderException("Malformed URL " + urlStr, e);
        }
    }


}
