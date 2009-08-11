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

import scriptella.configuration.ConfigurationException;
import scriptella.core.DriverFactory;
import scriptella.jdbc.GenericDriver;
import scriptella.spi.AbstractScriptellaDriver;
import scriptella.spi.Connection;
import scriptella.spi.ConnectionParameters;
import scriptella.util.CollectionUtils;
import scriptella.util.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Scriptella autodiscovery driver.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class Driver extends AbstractScriptellaDriver {
    private static Logger LOG = Logger.getLogger(Driver.class.getName());

    private static final Map<String, String> MAPPINGS = new HashMap<String, String>();

    private static final String AUTO_URL_PROPERTIES = "scriptella/driver/auto/url.properties";

    static {
        //Merge all autodiscovery.properties found. Similar to JAR SPI mechanism.
        try {
            List<URL> resources = Collections.list(Driver.class.getClassLoader().getResources(AUTO_URL_PROPERTIES));

            LOG.fine("Loading autodiscovery properties from " + resources);
            for (URL resource : resources) {
                Properties p = new Properties();
                p.load(resource.openStream());
                MAPPINGS.putAll(CollectionUtils.asMap(p));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize autodiscovery mappings", e);
        }
    }

    public Connection connect(final ConnectionParameters connectionParameters) throws ConfigurationException{
        String u = connectionParameters.getUrl();
        if (StringUtils.isEmpty(u)) {
            throw new ConfigurationException("url connection parameter is required");
        }
        //Finding an url in the mappings
        for (Map.Entry<String, String> entry : MAPPINGS.entrySet()) {
            String pattern = entry.getKey();
            String driver = entry.getValue();
            if (u.startsWith(pattern)) {
                return getConnection(driver, connectionParameters);
            }
        }
        if (u.startsWith("jdbc:")) { //If driver url not recognized - try JDBC 4.0 Auto-loading feature
            return getConnection(GenericDriver.class.getName(), connectionParameters);
        }
        throw new ConfigurationException("Unable to automatically discover driver for url " +
                connectionParameters.getUrl()+". Please explicitly specify a \"driver\" connection attribute.");
    }

    /**
     * Template method for testing.
     *
     * @param driver               driver class name or an alias.
     * @param connectionParameters conection parameters.
     * @return connection.
     */
    protected Connection getConnection(String driver, ConnectionParameters connectionParameters) {
        try {
            Connection c = DriverFactory.getDriver(driver, getClass().getClassLoader()).connect(connectionParameters);
            LOG.info("Using "+driver+" driver for url "+connectionParameters.getUrl());
            return c;
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Unable to initialize driver " + driver, e);
        }
    }
}
