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

import org.apache.commons.jexl2.JexlContext;
import scriptella.driver.script.ParametersCallbackMap;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

/**
 * Mutable {@link org.apache.commons.jexl2.JexlContext} implementation for
 * integration into Scriptella execution environment.
 * This class allows local variables to be set via {@link #set(String, Object)} method.
 * <br>{@link #get(String)} allows reading variables.
 * <p><b>Important:</b> This class is used instead of {@link org.apache.commons.jexl2.MapContext} because
 * due to parameters model limitations and performance reasons {@link #has(String)} method should always return true.
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public final class JexlContextMap implements JexlContext {
    private ParametersCallbackMap parametersMap;

    /**
     * Initializes instance and set parent parameters.
     *
     * @param parametersMap parent parameters.
     */
    public JexlContextMap(ParametersCallbackMap parametersMap) {
        this.parametersMap = parametersMap;
    }

    /**
     * Initializes instance and set parent parameters with query callback..
     *
     * @param parentParameters parent parameters.
     * @param queryCallback    query callback
     */
    public JexlContextMap(ParametersCallback parentParameters, QueryCallback queryCallback) {
        parametersMap = new ParametersCallbackMap(parentParameters, queryCallback);
    }


    public Object get(String name) {
        return parametersMap.get(name);
    }

    public void set(String name, Object value) {
        parametersMap.put(name, value);
    }

    public boolean has(String name) {
        //Current model does not allow to distinguish between null value and absence, so we assume
        //variable is always present, otherwise JEXL will log warnings and throws errors internally
        return true;
    }
}
