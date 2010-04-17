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
package scriptella.core;


/**
 * Base class for executable elements interceptors.
 * <p>Based on Chain-Of-Responsibility (GOF CoR) pattern.
 * <p>The purpose of this class is to provide an AOP-style approach for adding additional functionality.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 * @see DynamicContextDecorator
 */
public abstract class ElementInterceptor implements ExecutableElement {
    private ExecutableElement next;
    private DynamicContextDecorator ctxDecorator;

    protected ElementInterceptor(ExecutableElement next) {
        this.next = next;
    }

    protected ElementInterceptor(ExecutableElement next,
                                 DynamicContextDecorator ctxDecorator) {
        this.next = next;
        this.ctxDecorator = ctxDecorator;
    }

    protected DynamicContextDecorator getCtxDecorator() {
        return ctxDecorator;
    }

    protected ExecutableElement getNext() {
        return next;
    }

    /**
     * Executes next element in the chain.
     * @param ctx dynamic context.
     * @see ExecutableElement#execute(DynamicContext)
     */
    protected void executeNext(final DynamicContext ctx) {
        getNext().execute(ctx);
    }
}
