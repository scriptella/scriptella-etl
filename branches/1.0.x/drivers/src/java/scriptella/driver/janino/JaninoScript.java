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
package scriptella.driver.janino;

import scriptella.spi.ParametersCallback;

/**
 * A base class for Janino &lt;script&gt; elements.
 * <p>Public members of this class are available in Janino scripting elements.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class JaninoScript {
    /**
     * This field in not a part of the public API.
     */
    private ParametersCallback parametersCallback;

    /**
     * This method in not a part of the public API.
     */
    protected final ParametersCallback getParametersCallback() {
        return parametersCallback;
    }

    /**
     * This method in not a part of the public API.
     */
    final void setParametersCallback(ParametersCallback parametersCallback) {
        this.parametersCallback = parametersCallback;
    }

    /**
     * This method in not a part of the public API.
     */
    protected abstract void execute() throws Exception;


    /**
     * Obtains a parameter value for specified name.
     * <p>This method is available inside Janino &lt;script&gt; element.
     *
     * @param name parameter name
     * @return parameter value.
     */
    public final Object get(String name) {
        return parametersCallback.getParameter(name);
    }


}
