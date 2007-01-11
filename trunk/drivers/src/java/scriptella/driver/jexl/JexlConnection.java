/*
 * Copyright 2006-2007 The Scriptella Project Team.
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
package scriptella.driver.jexl;

import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.Script;
import org.apache.commons.jexl.ScriptFactory;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.IOUtils;

import java.io.IOException;

/**
 * Scriptella connection adapter for JEXL.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JexlConnection extends AbstractConnection {

    /**
     * Instantiates a new connection to Janino Script Evaluator.
     * @param parameters connection parameters.
     */
    public JexlConnection(ConnectionParameters parameters) {
        super(Driver.DIALECT, parameters);
    }

    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ProviderException {
        String s;
        try {
            s = IOUtils.toString(scriptContent.open());
        } catch (IOException e) {
            throw new JexlProviderException("Failed to read JEXL script", e);
        }
        try {
            Script script = ScriptFactory.createScript(s);
            JexlContext ctx = new JexlContextMap(parametersCallback);
            script.execute(ctx);
        } catch (Exception e) {
            throw new JexlProviderException("Failed to execute JEXL script", e);
        }
    }

    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException {


    }


    /**
     * Closes the connection and releases all related resources.
     */
    public void close() throws ProviderException {
    }

}
