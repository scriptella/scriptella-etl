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

import scriptella.spi.Connection;


/**
 * Allows to change behaviour of wrapped DynamicContext.
 * <p>Based on Decorator (from GOF) design pattern.
 * <p>Mostly intended to be used by {@link ElementInterceptor interceptors}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DynamicContextDecorator extends DynamicContext {
    private DynamicContext context;
    private Connection cachedConnection;

    public DynamicContextDecorator(DynamicContext context) {
        setContext(context);
    }

    /**
     * Specific constructor allows to set context later.
     * <p>Used for performance reasons to allow deferred initialization and dynamic context change.
     */
    DynamicContextDecorator() {
    }

    @Override
    public Object getParameter(final String name) {
        return context.getParameter(name);
    }

    @Override
    public Connection getConnection() {
        if (cachedConnection==null) {
            cachedConnection=context.getConnection();
        }
        return cachedConnection;
    }


    /**
     * Dynamically changes context beign decorated.
     * <p>Should be used with caution mostly for performance reasosns.
     *
     * @param context new context to decorate.
     */
    void setContext(final DynamicContext context) {
        this.context = context;
        globalContext = context.getGlobalContext();
        cachedConnection = null;
    }

}
