/*
 * Copyright 2006 The Scriptella Project Team.
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
package scriptella.sql;

import scriptella.expressions.ParametersCallback;
import scriptella.expressions.ThisParameter;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ResultSetAdapter implements ParametersCallback {
    private static final Pattern NUM_PTR = Pattern.compile("\\d+");
    private ResultSet resultSet;
    private Set<String> names;
    private ParametersCallback params;

    public ResultSetAdapter(ResultSet resultSet,
                            ParametersCallback parametersCallback) {
        this.params = parametersCallback;
        this.resultSet = resultSet;
        names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        try {
            final ResultSetMetaData m = resultSet.getMetaData();
            final int n = m.getColumnCount();

            for (int i = 1; i <= n; i++) {
                names.add(m.getColumnName(i));
            }
        } catch (SQLException e) {
        }
    }

    public Object getParameter(final String name) {
        if (ThisParameter.NAME.equals(name)) { //this could not be overriden

            return ThisParameter.get(params);
        }

        try {
            if (!names.contains(name)) {
                //If name is not a column name and is integer
                if (NUM_PTR.matcher(name).matches()) {
                    try {
                        int index = Integer.parseInt(name);

                        //Use name as index
                        return resultSet.getObject(index);
                    } catch (NumberFormatException e) {
                    }
                } else { //otherwise call previus params

                    return params.getParameter(name);
                }
            }

            return resultSet.getObject(name);
        } catch (SQLException e) {
            throw new JDBCException("Unable to get parameter " + name, e);
        }
    }
}
