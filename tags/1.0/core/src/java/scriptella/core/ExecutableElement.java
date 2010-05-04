/*
 * Copyright 2006-2009 The Scriptella Project Team.
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
 * Represents executable element like {@link QueryExecutor query} or {@link ScriptExecutor script}.
 * <p>{@link ElementInterceptor Interceptors} also implement this interface to act as proxies.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public interface ExecutableElement {
    /**
     * Executes the element.
     * <p><b>Note:</b> the context may be decorated by {@link DynamicContextDecorator}.
     *
     * @param ctx context to use.
     */
    void execute(final DynamicContext ctx);
}
