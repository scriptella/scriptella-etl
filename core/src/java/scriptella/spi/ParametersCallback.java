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
package scriptella.spi;


/**
 * Callback interface to obtain parameter values.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public interface ParametersCallback {
    /**
     * Returns the value of parameter specified by name.
     * <p>The callback internally delegates a call to parent callbacks if the parameter cannot be found.
     *
     * @param name parameter name. Providers are allowed (but not required) to ignore a case of the name parameter
     *             to comply with their internal model. For example JDBC drivers are case-insensitive to column names.
     * @return parameter value or null if parameter doesn't exist.
     */
    Object getParameter(final String name);
}
