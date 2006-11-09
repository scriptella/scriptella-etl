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


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class Query extends SqlSupport {
    public Query(Resource resource, JdbcConnection connection) {
        super(resource, connection);
    }

    public void execute(final ParametersCallback parametersCallback,
                        final QueryCallback queryCallback) {
        final int r;
        r = parseAndExecute(parametersCallback, queryCallback);

        if (r > 0) {
            throw new JdbcException("Query cannot make updates.");
        }
    }
}
