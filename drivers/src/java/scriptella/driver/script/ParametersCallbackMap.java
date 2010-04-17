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

import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * {@link java.util.Map} implementation of {@link ParametersCallback} for
 * integration into Scriptella execution environment.
 * <p/>
 * This class allows local variables to be set via {@link #put(String,Object)} method.
 * <br>{@link #getParameter(String)} allows reading variables.
 * <p>In query mode, a virtual variable <code>query</code> is available and exposes a method
 * {@link #next()} to populate result set.
 * </p>
 * <p><em>Note:</em> current implementation does not distinguish if a vairable is absent
 * or has a value of null.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ParametersCallbackMap implements ParametersCallback, Map<String, Object> {
    private Map<String, Object> localVariables;
    private ParametersCallback parentParameters;
    private QueryCallback queryCallback;


    /**
     * Initializes instance and set parent parameters to use in {@link #getParameter(String)}.
     *
     * @param parentParameters parent parameters.
     */
    public ParametersCallbackMap(ParametersCallback parentParameters) {
        this.parentParameters = parentParameters;
    }


    /**
     * Initializes parameters callback for query element.
     * @param parentParameters parent parameters.
     * @param queryCallback callback to notify on row iteration. 
     */
    public ParametersCallbackMap(ParametersCallback parentParameters, QueryCallback queryCallback) {
        this.parentParameters = parentParameters;
        setQueryCallback(queryCallback);
    }

    /**
     * Returns specified variable value.
     * <p>The local variables set by {@link #put(String,Object)} method
     * take priority of variables in parentParameters object.
     *
     * @param name variable name
     * @return value of variable or null if variable not found.
     */
    public Object getParameter(final String name) {
        Object v = localVariables == null ? null : localVariables.get(name);
        if (v != null) {
            return v;
        }
        return parentParameters.getParameter(name);
    }

    /**
     * Use {@link #getParameter(String)}.
     *
     * @param key variable name.
     * @return value of variable.
     */
    public Object get(Object key) {
        return (key instanceof String) ? getParameter((String) key) : null;
    }

    public boolean containsKey(Object key) {
        return (localVariables != null && localVariables.containsKey(key))
                || (parentParameters.getParameter((String) key) != null);
    }


    /**
     * Sets local variable.
     *
     * @param key   variable name.
     * @param value variable value.
     * @return previous variable value.
     */
    public Object put(String key, Object value) {
        if (localVariables == null) {
            localVariables = new HashMap<String, Object>();
        }
        return localVariables.put(key, value);
    }

    /**
     * Removes local variable.
     *
     * @param key variable name.
     * @return previous value.
     */
    public Object remove(Object key) {
        return localVariables == null ? null : localVariables.remove(key);
    }

    /**
     * Registers local variables.
     *
     * @param t local variables map.
     */
    public void putAll(Map<? extends String, ?> t) {
        if (localVariables == null) {
            localVariables = new HashMap<String, Object>();
        }
        localVariables.putAll(t);
    }

    /**
     * Clears local variables.
     */
    public void clear() {
        if (localVariables != null) {
            localVariables.clear();
        }
    }


    /**
     * Sets query callback and enables the query mode, i.e. query variable is exposed.
     * @param queryCallback query callback.
     */
    public void setQueryCallback(QueryCallback queryCallback) {
        this.queryCallback = queryCallback;
        put("query", this);
    }

    /**
     * Executes nested elements and exposes local variables set by the current query.
     */
    public void next() {
        queryCallback.processRow(this);
    }


    //Unsupported operations

    public int size() {
        throw new UnsupportedOperationException("size");
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException("isEmpty");
    }

    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("containsValue");
    }


    public Set<String> keySet() {
        throw new UnsupportedOperationException("keySet");
    }

    public Collection<Object> values() {
        throw new UnsupportedOperationException("values");
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException("entrySet");
    }
}
