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
package scriptella.spi.support;

import scriptella.spi.ParametersCallback;

/**
 * Hierarchical implementation of {@link scriptella.spi.ParametersCallback} interface.
 * <p>This class decorates behaviour of a primary callback by obtaining
 * absent parameter values from the parent callback.
 * <p><em>Note:</em> This class is mutable and not thread-safe.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class HierarchicalParametersCallback implements ParametersCallback {
    private ParametersCallback callback;
    private ParametersCallback parentCallback;


    /**
     * Creates a hierarchical parameters callback instance.
     *
     * @param callback       primary callback.
     * @param parentCallback secondary (parent) callback. If null - only primary callback is used.
     */
    public HierarchicalParametersCallback(ParametersCallback callback, ParametersCallback parentCallback) {
        setCallback(callback);
        setParentCallback(parentCallback);
    }


    /**
     * Returns primary callback.
     *
     * @return primary callback.
     */
    public ParametersCallback getCallback() {
        return callback;
    }

    /**
     * Sets primary callback.
     *
     * @param callback primary callback. Cannot be null.
     */
    public void setCallback(ParametersCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Parameters callback cannot be null");
        }
        this.callback = callback;
    }

    /**
     * Returns secondary callback.
     *
     * @return secondary callback.
     */
    public ParametersCallback getParentCallback() {
        if (callback == null) {
            throw new IllegalArgumentException("Parameters callback cannot be null");
        }
        return parentCallback;
    }

    /**
     * Sets secondary callback.
     *
     * @param parentCallback secondary callback. May be null, in this case only primary callback is used.
     */
    public void setParentCallback(ParametersCallback parentCallback) {
        this.parentCallback = parentCallback;
    }

    /**
     * This method obtains the value of a specified parameter from the primary callback
     * . If the returned value is null, the parameter's value is obtained from secondary callback.
     *
     * @param name parameter name.
     * @return value of the specified parameter.
     */
    public Object getParameter(final String name) {
        Object v = callback.getParameter(name);
        if (v != null) {
            return v;
        }
        return parentCallback == null ? null : parentCallback.getParameter(name);
    }
}
