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
package scriptella.driver.script;

import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * Adaptor {@link javax.script.ScriptEngine}. Provides helper methods and feature detection.
 *
 * @author Fyodor Kupolov
 */
class ScriptEngineWrapper implements Closeable {
    private ScriptEngine scriptEngine;
    private Compilable compilable;
    private boolean nashornScriptEngine;

    public ScriptEngineWrapper(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
        if (scriptEngine instanceof Compilable) {
            compilable = (Compilable) scriptEngine;
        }
        nashornScriptEngine = "jdk.nashorn.api.scripting.NashornScriptEngine".equals(scriptEngine.getClass().getName());
    }

    public boolean isNashornScriptEngine() {
        return nashornScriptEngine;
    }

    public boolean isCompilable() {
        return compilable != null;
    }

    public Compilable getCompilable() {
        if (compilable == null) {
            throw new IllegalStateException("Engine does not support Compilable");
        }
        return compilable;
    }

    public BindingsParametersCallback newBindingsParametersCallback(ParametersCallback parentParameters, QueryCallback queryCallback) {
        return nashornScriptEngine ? new NashornBindingsParametersCallback(parentParameters, queryCallback)
                : new BindingsParametersCallback(parentParameters, queryCallback);
    }

    public BindingsParametersCallback newBindingsParametersCallback(ParametersCallback parentParameters) {
        return nashornScriptEngine ? new NashornBindingsParametersCallback(parentParameters)
                : new BindingsParametersCallback(parentParameters);
    }

    public void evalNoCompile(Reader reader, BindingsParametersCallback bindings) throws ScriptException {
        scriptEngine.eval(reader, bindings);
    }

    public CompiledScript compile(Reader reader) throws ScriptException {
        return getCompilable().compile(reader);
    }

    @Override
    public void close() throws IOException {
        scriptEngine.getContext().getWriter().close();
    }
}
