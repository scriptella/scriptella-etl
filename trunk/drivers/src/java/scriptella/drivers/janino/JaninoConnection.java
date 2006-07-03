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

import scriptella.configuration.Resource;
import scriptella.expressions.ParametersCallback;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;

/**
 * Scriptella connection adapter for Janino Script Evaluator.
 */
public class JaninoConnection extends AbstractConnection {
    private CodeCompiler compiler = new CodeCompiler();

    /**
     * Instantiates a new connection to Janino Script Evaluator.
     */
    public JaninoConnection() {
        super(Driver.DIALECT_IDENTIFIER);
    }

    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ProviderException {
        JaninoScript s = compiler.compileScript(scriptContent);
        s.setParametersCallback(parametersCallback);
        try {
            s.eval();
        } catch (Exception e) {
            throw new JaninoProviderException("Script execution failed due to exception",e);
        } finally {
            //GC unused references
            s.setParametersCallback(null);
        }
    }

    /**
     * Executes a query specified by its content.
     * <p/>
     *
     * @param queryContent       query content.
     * @param parametersCallback callback to get parameter values.
     * @param queryCallback      callback to call for each result set element produced by this query.
     * @see #executeScript(scriptella.configuration.Resource, scriptella.expressions.ParametersCallback)
     */
    public synchronized void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException {

        JaninoQuery q = compiler.compileQuery(queryContent);
        q.setParametersCallback(parametersCallback);
        q.setQueryCallback(queryCallback);
        try {
            q.eval();
        } catch (Exception e) {
            throw new JaninoProviderException("Query execution failed due to exception",e);
        } finally {
            //GC unused references
            q.setParametersCallback(null);
            q.setQueryCallback(null);
        }
    }


    /**
     * Closes the connection and releases all related resources.
     */
    public void close() throws ProviderException {
        compiler=null;
    }

}
