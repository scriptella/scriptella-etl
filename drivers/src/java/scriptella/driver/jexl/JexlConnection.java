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
package scriptella.driver.jexl;

import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.Script;
import scriptella.driver.script.MissingQueryNextCallDetector;
import scriptella.driver.script.ParametersCallbackMap;
import scriptella.expression.JexlExpression;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.IOUtils;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Scriptella connection adapter for JEXL.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JexlConnection extends AbstractConnection {
    private Map<Resource, Script> cache = new IdentityHashMap<Resource, Script>();
    //Use the same factory method as in JexlExpression to share functions etc. 
    private static final JexlEngine jexlEngine = JexlExpression.newJexlEngine();

    /**
     * Instantiates a new connection to JEXL.
     *
     * @param parameters connection parameters.
     */
    public JexlConnection(ConnectionParameters parameters) {
        super(Driver.DIALECT, parameters);
    }

    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ProviderException {
        run(scriptContent, new JexlContextMap(new ParametersCallbackMap(parametersCallback)));
    }

    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException {
        final ParametersCallbackMap parametersMap = new ParametersCallbackMap(parametersCallback, queryCallback);
        MissingQueryNextCallDetector detector = new MissingQueryNextCallDetector(parametersMap, queryContent);
        run(queryContent, new JexlContextMap(parametersMap));
        detector.detectMissingQueryNextCall();
    }



    private void run(Resource resource, JexlContextMap ctx) {
        Script script = cache.get(resource);
        if (script == null) {
            String s;
            try {
                s = IOUtils.toString(resource.open());
            } catch (IOException e) {
                throw new JexlProviderException("Unable to open resource", e);
            }

            try {
                cache.put(resource, script = jexlEngine.createScript(s));
            } catch (Exception e) {
                throw new JexlProviderException("Failed to compile JEXL script", e);
            }
        }
        try {
            script.execute(ctx);
        } catch (Exception e) {
            throw new JexlProviderException("Failed to execute JEXL script", e);
        }
    }

    /**
     * Closes the connection and releases all related resources.
     */
    public void close() throws ProviderException {
        cache = null;
    }

}
