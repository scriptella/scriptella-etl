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
package scriptella.jdbc;

import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;

import java.sql.Connection;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class Query extends SQLSupport {
    public Query(final String sql) {
        super(sql);
    }

    public Query(Resource resource) {
        super(resource);
    }

    public void execute(final Connection connection,
                        final QueryCallback callBack) {
        execute(connection, null, callBack);
    }

    public void execute(final Connection connection, final ParametersCallback parametersCallback,
                        final QueryCallback queryCallback) {
        final int r;
        r = parseAndExecute(connection, parametersCallback, queryCallback);

        if (r > 0) {
            throw new JDBCException("Query cannot make updates.");
        }
    }
}
