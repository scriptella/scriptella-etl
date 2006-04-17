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


/**
 * Base class for SQL elements interceptors.
 * <p>Based on Chain-Of-Responsibility (GOF CoR) pattern.
 * <p>The purpose of this class is to provide an AOP-style approach for adding additional functionality.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 * @see SQLContextDecorator
 */
public abstract class SQLElementInterceptor implements SQLExecutableElement {
    private SQLExecutableElement next;
    private SQLContextDecorator ctxDecorator;

    protected SQLElementInterceptor(SQLExecutableElement next) {
        this.next = next;
    }

    protected SQLElementInterceptor(SQLExecutableElement next,
                                    SQLContextDecorator ctxDecorator) {
        this.next = next;
        this.ctxDecorator = ctxDecorator;
    }

    protected SQLContextDecorator getCtxDecorator() {
        return ctxDecorator;
    }

    protected SQLExecutableElement getNext() {
        return next;
    }

    protected void executeNext(final SQLContext ctx) {
        getNext().execute(ctx);
    }
}
