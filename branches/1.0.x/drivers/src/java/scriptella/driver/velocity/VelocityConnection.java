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
package scriptella.driver.velocity;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;
import scriptella.driver.text.AbstractTextConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Level;

/**
 * Represents a session to velocity engine.
 */
public class VelocityConnection extends AbstractTextConnection {
    public static final String OUTPUT_ENCODING = "output.encoding";
    private final VelocityEngine engine;
    private final VelocityContextAdapter adapter;
    private Writer writer;//lazy initialized


    /**
     * Instantiates a velocity connection.
     *
     * @param parameters connection parameters.
     */
    public VelocityConnection(ConnectionParameters parameters) {
        super(Driver.DIALECT, parameters);
        engine = new VelocityEngine();
        engine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, LOG_SYSTEM);
        engine.setProperty("velocimacro.library", "");//unnecessary file in our case
        try {
            engine.init();
        } catch (Exception e) {
            throw new VelocityProviderException("Unable to initialize engine", e);
        }
        adapter = new VelocityContextAdapter();
    }

    /**
     * Executes a script specified by its content.
     * <p>scriptContent may be used as a key for caching purposes, i.e.
     * provider may precompile scripts and use compiled versions for subsequent executions.
     * <p>This method is synchronized to to prevent multiple threads from working with the same writer.
     * Additionally single velocityEngine and context adapter instances are used.
     *
     * @param scriptContent      script content.
     * @param parametersCallback callback to get parameter values.
     */
    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ProviderException {
        //todo Current solution is slow, use per scriptContent caching by providing a custom Velocity ResourceLoader
        //todo also make Resource identifiable, i.e. replace url.getFile with resource name/location
        adapter.setCallback(parametersCallback);//we may use single context+engine because method is synchronized
        Reader reader = null;
        try {
            reader = scriptContent.open();
            Writer w = getWriter();
            engine.evaluate(adapter, w, url == null ? "System.out" : url.getFile(), reader);
            if (flush) {
                w.flush();
            }
        } catch (Exception e) {
            throw new VelocityProviderException("Unable to execute script", e);
        } finally {
            adapter.setCallback(null);//cleaning up to avoid mem leaks
            IOUtils.closeSilently(reader);
        }
    }

    /**
     * Executes a query specified by its content.
     * <p/>
     *
     * @param queryContent       query content.
     * @param parametersCallback callback to get parameter values.
     * @param queryCallback      callback to call for each result set element produced by this query.
     * @see #executeScript(scriptella.spi.Resource,scriptella.spi.ParametersCallback)
     */
    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException {
        throw new UnsupportedOperationException("Query execution is not supported yet");
    }

    private Writer getWriter() {
        if (writer == null) {
            try {
                writer = IOUtils.asBuffered(newOutputWriter());
            } catch (IOException e) {
                throw new VelocityProviderException("Unable to open URL " + url + " for output", e);
            }
        }
        return writer;
    }


    /**
     * Closes the connection and releases all related resources.
     */
    public synchronized void close() throws ProviderException {
        if (writer != null) {
            IOUtils.closeSilently(writer);
            writer = null;
        }
    }

    //Adapting classes
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
                case ERROR_ID:
                    lev = Level.WARNING;
                    break;
                default:
                    lev = Level.INFO;
            }
            if (Driver.LOG.isLoggable(lev)) {
                Driver.LOG.log(lev, "Engine: " + message);
            }
        }
    };


    /**
     * Velocity Context adapter class for {@link ParametersCallback}.
     */
    private static class VelocityContextAdapter implements Context {
        private ParametersCallback callback;

        public void setCallback(ParametersCallback callback) {
            this.callback = callback;
        }

        public Object put(String key, Object value) {
            throw new UnsupportedOperationException();
        }

        public Object get(String key) {
            return callback.getParameter(key);
        }

        public boolean containsKey(Object key) {
            return false;
        }

        public Object[] getKeys() {
            throw new UnsupportedOperationException();
        }

        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }
    }

}
