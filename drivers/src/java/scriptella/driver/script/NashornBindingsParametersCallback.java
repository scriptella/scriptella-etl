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

import javax.script.Bindings;
import java.util.*;

/**
 * Implementation of {@link javax.script.Bindings} for Java 8 JavaScript engine "Nashorn".
 * <p>See <a href="https://wiki.openjdk.java.net/display/Nashorn/Nashorn+jsr223+engine+notes">Nashorn engine notes</a>
 * for reasoning. In essence, Nashorn expects {@link javax.script.Bindings} to be an instance of
 * {@code jdk.nashorn.api.scripting.ScriptObjectMirror}, otherwise it puts "nashorn.global" variable inside and use it
 * to store all variables. This makes the interface pretty unusable and requires some tricks implemented below</p>
 *
 * TODO: Fix issue with outer scripts variables not being visible to Nashorn. https://github.com/scriptella/scriptella-etl/issues/2
 *
 * @author Fyodor Kupolov
 */
public class NashornBindingsParametersCallback extends BindingsParametersCallback {
    private Bindings nashornGlobal;
    private Set<String> exportVars = new HashSet<String>(Arrays.asList("f1", "localProp"));

    public NashornBindingsParametersCallback(ParametersCallback parentParameters) {
        super(parentParameters);
    }

    public NashornBindingsParametersCallback(ParametersCallback parentParameters, QueryCallback queryCallback) {
        super(parentParameters, queryCallback);
    }

    @Override
    public Object getParameter(String name) {
        if (nashornGlobal != null && nashornGlobal.containsKey(name)) {
            return nashornGlobal.get(name);
        }
        return super.getParameter(name);
    }

    @Override
    public Object get(Object key) {
        return super.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(key);
    }

    @Override
    public Object put(String key, Object value) {
        if (value instanceof Bindings && "nashorn.global".equals(key)) {
            nashornGlobal = (Bindings) value;
        }
        return super.put(key, value);
    }
}
