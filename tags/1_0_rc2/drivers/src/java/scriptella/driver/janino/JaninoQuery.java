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
package scriptella.driver.janino;

import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.util.CollectionUtils;

import java.util.Map;

/**
 * A base class for Janino &lt;query&gt; elements.
 * <p>This class exposes a simplified query API. The concept of this API is the following:
 * <ul>
 * <li>A virtual row is produced by calling {@link #set(String, Object)}
 * for each virtual column.
 * <li>{@link #next()} is called to process this row by nested scripting elements.
 * </ul>
 * Additionally a {@link #next(String[], Object[]) helper method} exists to make iterating even simpler.
 * <p>Virtual rows may also be {@link #set(java.util.Map) constructed} from {@link Map} or {@link java.util.Properties}.
 * <p>Public members of this class are available in Janino scripting elements.
 * <p>Examples:
 * <p>The following query produces two virtual rows:</p>
 * <table border=1>
 * <tr><th>id</th><th>name</th><th>age</th></tr>
 * <tr><td>123</td><td>John</td><td>20</td></tr>
 * <tr><td>200</td><td>Mary</td><td></td></tr>
 * </table>
 *
 * <code><pre>
 * &lt;query&gt;
 *    set("id", "123"); //set row column
 *    set("name", "John");
 *    set("age", new Integer(20));
 *    next();//sends a virtual row for processing
 *    set("id", "200");
 *    set("name", "Mary");
 *    next();
 * &lt;/query&gt;
 * </pre></code>
 * The same effect may achieved using the following code:
 * <code><pre>
 * &lt;query&gt;
 *    String[] names = new String[] {"id", "name", "age"};
 *    next(names, new Object[] {"123", "John", new Integer(20)};
 *    next(names, new Object[] {"200", "Mary", null)};
 * &lt;/query&gt;
 * </pre></code>
 * <p>Assume you have a map(or Properties) with the following mapping:
 * <br><code>id->123, name->John, age->20</code>
 * <br>A virtual row is produced using this code:
 * <code><pre>
 * &lt;query&gt;
 *    //fill a map or load properties file
 *    next(map};
 * &lt;/query&gt;
 * </pre></code>
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class JaninoQuery extends JaninoScript implements ParametersCallback {
    private QueryCallback queryCallback;
    private Map<String, Object> row;

    /**
     * This method in not a part of the public API.
     */
    final void setQueryCallback(QueryCallback queryCallback) {
        this.queryCallback = queryCallback;
    }

    public final QueryCallback getQueryCallback() {
        return queryCallback;
    }

    public final Object getParameter(final String name) {
        if (row != null) {
            Object v = row.get(name);
            if (v != null) {
                return v;
            }
        }
        return super.get(name);
    }


    /**
     * Sets a value for specified parameter name.
     * <p>This parameter becomes visible to nested scripting elements
     * after {@link #next()} method is called.
     * <p>This method is available inside Janino &lt;query&gt; element.
     * @param name parameter name
     * @param value parameter value.
     */
    public final void set(String name, Object value) {
        initRow();
        row.put(name, value);
    }

    /**
     * Fills the virtual row using parameters from specified map.
     * <p>This method is available inside Janino &lt;query&gt; element.
     * @param parametersMap map of parameters, where key is a variable name
     */
    public final void set(Map<String,?> parametersMap) {
        initRow();
        row.putAll(parametersMap);
    }

    private void initRow() {
        if (row == null) {
            row = CollectionUtils.newCaseInsensitiveAsciiMap();
        }
    }

    /**
     * Moves to the next virtual row.
     * <p>Nested scripting elements are evaluated and
     * the parameters set by {@link #set(String, Object)} method are available to them.
     * <p><em>Note:</em> The values of all parameters set via {@link #set(String, Object)} method are
     * cleared(or restored).
     * <p>This method is available inside Janino &lt;query&gt; element.
     */
    public final void next() {
        queryCallback.processRow(this);
        if (row != null) {
            row.clear();
        }
    }

    /**
     * Produces a virtual row based on the specified columns.
     * <p>A serie of {@link #set(String, Object) parameter setters} is performed.
     * After parameters for the current row have been set, {@link #next()} method is invoked.
     * <p>This method is available inside Janino &lt;query&gt; element.
     * @param parameterNames array of parameter names.
     * @param values array of parameter values, i.e. value[i] specifies a value for parameter[i].
     */
    public final void next(String[] parameterNames, Object[] values) {
        for (int i = 0; i < parameterNames.length; i++) {
            set(parameterNames[i], values[i]);
        }
        next();
    }

    /**
     * Produces a virtual row based on the specified columns.
     * <p>Parameters are set via {@link #set(java.util.Map)} method.
     * After parameters for the current row have been set, {@link #next()} method is invoked.
     * <p>This method is available inside Janino &lt;query&gt; element.
     * @param parametersMap map of parameters.
     */
    public final void next(Map<String,?> parametersMap) {
        set(parametersMap);
        next();
    }



    public String toString() {
        return "Query";
    }


}
