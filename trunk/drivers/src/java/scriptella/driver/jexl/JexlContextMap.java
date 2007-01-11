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
import scriptella.spi.ParametersCallback;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mutable {@link org.apache.commons.jexl.JexlContext} implementation for
 * integration into Scriptella execution environment.
 * This class allows local variables to be set via {@link #put(String,Object)} method.
 * <br>{@link #getParameter(String)} allows reading variables.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JexlContextMap implements ParametersCallback, JexlContext, Map<String, Object> {
    private Map<String, Object> localVariables = new HashMap<String, Object>();
    private ParametersCallback parentParameters;


    /**
     * Initializes instance and set parent parameters to use in {@link #getParameter(String)}.
     *
     * @param parentParameters parent parameters.
     */
    public JexlContextMap(ParametersCallback parentParameters) {
        this.parentParameters = parentParameters;
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
        Object v = localVariables.get(name);
        if (v != null) {
            return v;
        }
        return parentParameters.getParameter(name);
    }

    @SuppressWarnings("unchecked")
    public void setVars(Map vars) {
        localVariables.clear();
        localVariables.putAll(vars);
    }

    public Map getVars() {
        return this;
    }

    /**
     * Use {@link #getParameter(String)}.
     * @param key variable name.
     * @return value of variable.
     */
    public Object get(Object key) {
        return (key instanceof String) ? getParameter((String) key) : null;
    }

    /**
     * Sets local variable.
     * @param key variable name.
     * @param value variable value.
     * @return previous variable value.
     */
    public Object put(String key, Object value) {
        return localVariables.put(key, value);
    }

    /**
     * Removes local variable.
     * @param key variable name.
     * @return previous value.
     */
    public Object remove(Object key) {
        return localVariables.remove(key);
    }

    /**
     * Registers local variables.
     * @param t local variables map.
     */
    public void putAll(Map<? extends String, ?> t) {
        localVariables.putAll(t);
    }

    /**
     * Clears local variables.
     */
    public void clear() {
        localVariables.clear();
    }

    //Unsupported operations

    public int size() {
        throw new UnsupportedOperationException();
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException();
    }

    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }


    public Set<String> keySet() {
        throw new UnsupportedOperationException();
    }

    public Collection<Object> values() {
        throw new UnsupportedOperationException();
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException();
    }
}
