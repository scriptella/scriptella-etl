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
package scriptella.driver.janino;

import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;

/**
 * Scriptella connection adapter for Janino Script Evaluator.
 */
public class JaninoConnection extends AbstractConnection {
    private CodeCompiler compiler = new CodeCompiler();

    /**
     * Instantiates a new connection to Janino Script Evaluator.
     * @param parameters connection parameters.
     */
    public JaninoConnection(ConnectionParameters parameters) {
        super(Driver.DIALECT_IDENTIFIER, parameters);
    }

    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ProviderException {
        JaninoScript s = compiler.compileScript(scriptContent);
        s.setParametersCallback(parametersCallback);
        try {
            s.execute();
        } catch (Exception e) {
            throw guessErrorStatement(new JaninoProviderException(
                    "Script execution failed due to exception",e), scriptContent, s.getClass());
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
     * @see #executeScript(scriptella.spi.Resource, scriptella.spi.ParametersCallback)
     */
    public synchronized void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException {

        JaninoQuery q = null;
        q = compiler.compileQuery(queryContent);
        q.setParametersCallback(parametersCallback);
        q.setQueryCallback(queryCallback);
        try {
            q.execute();
        } catch (Exception e) {
            throw guessErrorStatement(new JaninoProviderException(
                    "Query execution failed due to exception",e), queryContent, q.getClass());
        } finally {
            //GC unused references
            q.setParametersCallback(null);
            q.setQueryCallback(null);
        }
    }

    /**
     * Finds an error statement by introspecting the stack trace of exception.
     * <p>Error statement is fetched from content resource.
     * @param content
     * @param e exception to introspect.
     * @param scriptClass class to use when finding an error statement.
     * @return the same exception with a guessed error statement.
     */
    private static JaninoProviderException guessErrorStatement(JaninoProviderException e, Resource content, Class scriptClass) {
        Throwable ex = e.getCause();
        StackTraceElement[] trace = ex.getStackTrace();
        final String scriptClassName = scriptClass.getName();
        for (StackTraceElement el : trace) { //find the top most instance of generated class
            String className = el.getClassName();
            if (scriptClassName.equals(className)) {
                String st = CodeCompiler.getLine(content, el.getLineNumber());
                e.setErrorStatement(st);
                break;
            }

        }
        return e;
    }

    /**
     * Closes the connection and releases all related resources.
     */
    public void close() throws ProviderException {
        compiler=null;
    }

}
