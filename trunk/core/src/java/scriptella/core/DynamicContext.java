/*
 * Copyright 2006-2012 The Scriptella Project Team.
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
package scriptella.core;

import scriptella.execution.EtlContext;
import scriptella.spi.Connection;
import scriptella.spi.ParametersCallback;


/**
 * Represents dynamic execution context for executable elements.
 * <p>TODO: Probably it's better to extract interface from this class
 * and merge implementation with {@link scriptella.execution.EtlContext}
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
@ThreadSafe
public class DynamicContext implements ParametersCallback {
    protected EtlContext globalContext;
    protected EtlVariable etlVariable;

    protected DynamicContext() {
    }

    public DynamicContext(EtlContext globalContext) {
        this.globalContext = globalContext;
    }

    public Object getParameter(final String name) {
        if (EtlVariable.NAME.equals(name)) {
            return getEtlVariable();
        }
        return globalContext.getParameter(name);
    }

    public Connection getConnection() {
        final ConnectionManager cf = globalContext.getSession()
                .getConnection(null);
        return cf.getConnection();
    }

    protected EtlContext getGlobalContext() {
        return globalContext;
    }

    EtlVariable getEtlVariable() {
        if (etlVariable == null) {
            etlVariable = new EtlVariable(this, globalContext.getGlobalVariables());
        }
        return etlVariable;
    }
}
