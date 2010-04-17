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
import scriptella.driver.script.ParametersCallbackMap;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.util.Map;

/**
 * Mutable {@link org.apache.commons.jexl.JexlContext} implementation for
 * integration into Scriptella execution environment.
 * This class allows local variables to be set via {@link #put(String,Object)} method.
 * <br>{@link #getParameter(String)} allows reading variables.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public final class JexlContextMap extends ParametersCallbackMap implements JexlContext {
    /**
     * Initializes instance and set parent parameters to use in {@link #getParameter(String)}.
     *
     * @param parentParameters parent parameters.
     */
    public JexlContextMap(ParametersCallback parentParameters) {
        super(parentParameters);
    }


    public JexlContextMap(ParametersCallback parentParameters, QueryCallback queryCallback) {
        super(parentParameters, queryCallback);
    }

    @SuppressWarnings("unchecked")
    public void setVars(Map vars) {
        clear();
        putAll(vars);
    }

    public Map<String, Object> getVars() {
        return this;
    }

}
