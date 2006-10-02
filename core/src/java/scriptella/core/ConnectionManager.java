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
package scriptella.core;

import scriptella.configuration.ConfigurationException;
import scriptella.configuration.ConnectionEl;
import scriptella.execution.EtlContext;
import scriptella.spi.Connection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.DriverClassLoader;
import scriptella.spi.DriverFactory;
import scriptella.spi.ScriptellaDriver;
import scriptella.util.UrlPathTokenizer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConnectionManager {
    private static final Logger LOG = Logger.getLogger(ConnectionManager.class.getName());
    Connection connection;
    List<Connection> newConnections;
    private ScriptellaDriver driver;
    ConnectionParameters connectionParameters;

    public ConnectionManager(EtlContext ctx, ConnectionEl c) {

        String cat = ctx.substituteProperties(c.getCatalog());
        String drvClass = ctx.substituteProperties(c.getDriver());
        String pwd = ctx.substituteProperties(c.getPassword());
        String user = ctx.substituteProperties(c.getUser());
        String schema = ctx.substituteProperties(c.getSchema());
        String url = ctx.substituteProperties(c.getUrl());
        String cp = ctx.substituteProperties(c.getClasspath());

        //Obtains a classloader
        ClassLoader cl = getClass().getClassLoader();
        if (cp!=null) { //if classpath specified
            //Parse it and create a new classloader
            UrlPathTokenizer tok = new UrlPathTokenizer(ctx.getScriptFileURL());
            try {
                URL[] urls = tok.split(cp);
                if (urls.length>0) {
                    cl = new DriverClassLoader(urls);
                }
            } catch (MalformedURLException e) {
                throw new ConfigurationException("Unable to parse classpath parameter for "+connection, e);
            }
        }
        connectionParameters = new ConnectionParameters(c.getProperties(), url, user, pwd, schema, cat, ctx);
        try {
            driver = DriverFactory.getDriver(drvClass, cl);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Driver class " + drvClass + " not found for " + connectionParameters +
                    ".Please check if the class name is correct and required libraries available on classpath", e);
        } catch (Exception e) {
            throw new ConfigurationException("Unable to initialize driver for " + connectionParameters + ":" + e.getMessage(), e);
        }
    }

    public Connection getConnection() {
        if (connection == null) {
            connection = driver.connect(connectionParameters);
        }

        return connection;
    }

    public Connection newConnection() {
        final Connection c = driver.connect(connectionParameters);

        if (newConnections == null) {
            newConnections = new ArrayList<Connection>();
        }

        newConnections.add(c);

        return c;
    }

    public void rollback() {
        for (Connection c : getAllConnections()) {
            try {
                c.rollback();
            } catch (UnsupportedOperationException e) {
                String msg = e.getMessage();
                LOG.log(Level.WARNING,
                        "Unable to rollback transaction for connection " + c + (msg==null?"": ": " + msg));

            } catch (Exception e) {
                LOG.log(Level.WARNING,
                        "Unable to rollback transaction for connection " + c, e);
            }
        }
    }

    public void commit() {
        for (Connection c : getAllConnections()) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Commiting connection "+c);
            }
            c.commit();
        }
    }


    public void close() {
        for (Connection c : getAllConnections()) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Problem occured while trying to close connection " + c, e);
                }
            }
        }

        connection = null;
        newConnections = null;
        connectionParameters = null;
        driver = null;
    }

    /**
     * @return connection and newtx connections
     */
    private List<Connection> getAllConnections() {
        List<Connection> cl = new ArrayList<Connection>();

        if (newConnections != null) {
            cl.addAll(newConnections);
        }

        if (connection != null) {
            cl.add(connection);
        }
        return cl;
    }



}
