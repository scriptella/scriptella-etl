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
package scriptella.driver.script;

import scriptella.configuration.ConfigurationException;
import scriptella.expression.LineIterator;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.DialectIdentifier;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.ExceptionUtils;
import scriptella.util.IOUtils;
import scriptella.util.StringUtils;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Scriptella connection adapter for the JSR 223: Scripting for the Java™ Platform.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 * <p/>
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptConnection extends AbstractConnection {
    private static final Logger LOG = Logger.getLogger(ScriptConnection.class.getName());
    private Map<Resource, CompiledScript> cache;
    private ScriptEngine engine;
    private Compilable compiler;
    private String encoding;
    private URL url;
    private Writer out;
    /**
     * Name of the <code>language</code> connection property.
     */
    static final String LANGUAGE = "language";

    /**
     * Name of the <code>encoding</code> connection property.
     * Specifies charset encoding of a character stream specified by an url connection parameter.
     */
    static final String ENCODING = "encoding";


    /**
     * Instantiates a new connection to JSR 223 scripting engine.
     *
     * @param parameters connection parameters.
     */
    public ScriptConnection(ConnectionParameters parameters) {
        super(parameters);
        String lang = parameters.getStringProperty(LANGUAGE);
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager(ScriptConnection.class.getClassLoader());
        if (StringUtils.isEmpty(lang)) { //JavaScript is used by default
            LOG.fine("Script language was not specified. JavaScript is default.");
            lang = "js";
        }

        engine = scriptEngineManager.getEngineByName(lang);
        if (engine == null) {
            throw new ConfigurationException("Specified " + LANGUAGE + "=" + lang + " not supported. Available values are: " +
                    getAvailableEngines(scriptEngineManager));
        }
        if (engine instanceof Compilable) {
            compiler = (Compilable) engine;
            cache = new IdentityHashMap<Resource, CompiledScript>();
        } else {
            LOG.info("Engine " + engine.getFactory().getEngineName() + " does not support compilation. Running in interpreted mode.");
        }
        if (!StringUtils.isEmpty(parameters.getUrl())) { //if url is specified
            url = parameters.getResolvedUrl();
            //setup reader and writer for it
            ScriptContext ctx = engine.getContext();
            ctx.setReader(new LazyReader());
            //JS engine bug - we have to wrap with PrintWriter, because otherwise print function won't work.
            ctx.setWriter(new PrintWriter(new LazyWriter()));
        }
        encoding = parameters.getCharsetProperty(ENCODING);
        ScriptEngineFactory f = engine.getFactory();
        setDialectIdentifier(new DialectIdentifier(f.getLanguageName(), f.getLanguageVersion()));

    }

    /**
     * Returns available script engine names including all aliases.
     *
     * @param manager script manager instance to use.
     * @return list of languages each of them represented by a list of names..
     */
    static List<List<String>> getAvailableEngines(ScriptEngineManager manager) {
        List<List<String>> list = new ArrayList<List<String>>();
        for (ScriptEngineFactory scriptEngineFactory : manager.getEngineFactories()) {
            list.add(scriptEngineFactory.getNames());
        }
        return list;
    }

    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ScriptProviderException, ConfigurationException {
        run(scriptContent, new BindingsParametersCallback(parametersCallback));
    }

    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ScriptProviderException, ConfigurationException {
        run(queryContent, new BindingsParametersCallback(parametersCallback, queryCallback));
    }

    /**
     * Compiles and runs the specified resource.
     *
     * @param resource           resource to compile.
     * @param parametersCallback parameters callback.
     */
    private void run(Resource resource, BindingsParametersCallback parametersCallback) {
        try {
            if (compiler == null) {
                engine.eval(resource.open(), parametersCallback);
                return;
            }
            CompiledScript script = cache.get(resource);
            if (script == null) {
                try {
                    cache.put(resource, script = compiler.compile(resource.open()));
                } catch (ScriptException e) {
                    throw new ScriptProviderException("Failed to compile script", e, getErrorStatement(resource, e));
                }
            }
            script.eval(parametersCallback);
        } catch (IOException e) {
            throw new ScriptProviderException("Failed to open script for reading", e);
        } catch (ScriptException e) {
            throw new ScriptProviderException("Failed to execute script", e, getErrorStatement(resource, e));
        }

    }

    static String getErrorStatement(Resource resource, ScriptException exception) {
        LineIterator it = null;
        try {
            it = new LineIterator(resource.open());
            return it.getLineAt(exception.getLineNumber() - 1);
        } catch (IOException e) {
            ExceptionUtils.ignoreThrowable(e);
        } finally {
            IOUtils.closeSilently(it);
        }
        return null;
    }


    /**
     * Closes the connection and releases all related resources.
     */
    public void close() throws ProviderException {
        cache = null;
    }

    /**
     * Lazily initialized reader.
     */
    final class LazyReader extends Reader {
        private Reader r;

        public int read(char cbuf[], int off, int len) throws IOException {
            if (r == null) {
                r = IOUtils.getReader(url.openStream(), encoding);
            }
            return r.read(cbuf, off, len);
        }

        public void close() throws IOException {
            if (r != null) {
                r.close();
            }
        }
    }

    /**
     * Lazily initialized writer which appends chars to an URL stream.
     */
    final class LazyWriter extends Writer {

        public void write(char cbuf[], int off, int len) throws IOException {
            if (out == null) {
                out = IOUtils.getWriter(IOUtils.getOutputStream(url), encoding);
            }
            out.write(cbuf, off, len);
        }

        public void flush() throws IOException {
            if (out != null) {
                out.flush();
            }
        }

        public void close() throws IOException {
            if (out != null) {
                out.close();
            }

        }
    }

}
