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

import java.sql.Connection;


/**
 * Allows to change behaviour of wrapped SQLContext.
 * <p>Based on Decorator (from GOF) design pattern.
 * <p>Mostly intended to be used by {@link SQLElementInterceptor interceptors}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SQLContextDecorator extends SQLContext {
    private SQLContext context;

    public SQLContextDecorator(SQLContext context) {
        setContext(context);
    }

    /**
     * Specific constructor allows to set context later.
     * <p>Used for performance reasons to allow deferred initialization and dynamic context change.
     */
    SQLContextDecorator() {
    }

    @Override
    public Object getParameter(final String name) {
        return context.getParameter(name);
    }

    @Override
    public Connection getConnection() {
        return context.getConnection();
    }

    public Connection getNewConnection() {
        return context.getNewConnection();
    }

    @Override
    public DialectIdentifier getDialectIdentifier() {
        return context.getDialectIdentifier();
    }

    /**
     * Dynamically changes context beign decorated.
     * <p>Should be used with caution mostly for performance reasosns.
     *
     * @param context new context to decorate.
     */
    void setContext(final SQLContext context) {
        this.context = context;
        globalContext = context.globalContext;
    }
}
