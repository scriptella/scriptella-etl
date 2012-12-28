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
package scriptella.driver.scriptella;

import scriptella.configuration.ConfigurationFactory;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;
import scriptella.execution.ExecutionStatistics;
import scriptella.expression.LineIterator;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.DriverContext;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.StringUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a connection to externally located Scriptella ETL file.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptellaConnection extends AbstractConnection {
    private static final Logger LOG = Logger.getLogger(ScriptellaConnection.class.getName());
    private ConfigurationFactory configurationFactory = new ConfigurationFactory();
    private DriverContext ctx;



    /**
     * Creates an email connection.
     *
     * @param parameters connection parameters.
     */
    public ScriptellaConnection(ConnectionParameters parameters) {
        super(Driver.DIALECT, parameters);
        ctx = parameters.getContext();
        //If url attribute specified - execute this file
        if (!StringUtils.isEmpty(parameters.getUrl())) {
            execute(parameters.getResolvedUrl(), ctx);
        }
    }

    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ProviderException {
        LineIterator it;
        try {
            it = new LineIterator(scriptContent.open(), new PropertiesSubstitutor(parametersCallback), true);
        } catch (IOException e) {
            throw new ScriptellaProviderException("Unable to open script", e);
        }

        while (it.hasNext()) {
            String uri = it.next();
            if (StringUtils.isEmpty(uri)) { //skipping empty lines as they are resolved to a main ETL file
                continue;
            }
            try {
                execute(ctx.resolve(uri), parametersCallback);
            } catch (MalformedURLException e) {
                throw new ScriptellaProviderException("Malformed URI " + uri, e);
            }
        }
    }

    private void execute(URL u, ParametersCallback callback) {
        //For now we support recursive calls. Uncomment the following line to disable recursive calls.
//        if (ctx.getScriptFileURL().equals(u)) {
//            throw new ScriptellaProviderException("Recursive calls not supported");
//        }
        if (isReadonly()) {
            LOG.info("Readonly Mode - Skipping ETL file " + u);
        } else {
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Executing Scriptella ETL file " + u);
                }
                configurationFactory.setResourceURL(u);
                configurationFactory.setExternalParameters(callback);
                ExecutionStatistics st = new EtlExecutor(configurationFactory.createConfiguration()).execute();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Completed ETL file execution.\n" + st);
                }
            } catch (EtlExecutorException e) {
                throw new ScriptellaProviderException("Failed to execute script " + u + " : " + e.getMessage(), e);
            }
        }
    }

    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException {
        throw new ScriptellaProviderException("Queries are not supported");
    }

    public void close() throws ProviderException {
    }
}
