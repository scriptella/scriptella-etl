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
package scriptella.core;

import scriptella.configuration.ContentEl;
import scriptella.configuration.QueryEl;
import scriptella.configuration.ScriptEl;
import scriptella.configuration.ScriptingElement;
import scriptella.expressions.ParametersCallback;
import scriptella.spi.Connection;
import scriptella.spi.QueryCallback;

import java.util.ArrayList;
import java.util.List;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class QueryExecutor extends ContentExecutor<QueryEl> {
    private List<ExecutableElement> nestedElements;

    private QueryExecutor(QueryEl queryEl) {
        super(queryEl);
        initNestedExecutors();
    }

    private void initNestedExecutors() {
        final List<ScriptingElement> childElements = getElement().getChildScriptinglElements();
        nestedElements = new ArrayList<ExecutableElement>(childElements.size());

        for (ScriptingElement element : childElements) {
            if (element instanceof QueryEl) {
                nestedElements.add(QueryExecutor.prepare((QueryEl) element));
            } else if (element instanceof ScriptEl) {
                nestedElements.add(ScriptExecutor.prepare((ScriptEl) element));
            } else {
                throw new IllegalStateException("Type " + element.getClass() +
                        " not supported");
            }
        }
    }

    public void execute(final DynamicContext ctx) {
        final Connection c = ctx.getConnection();

        final ContentEl content = getContent(c.getDialectIdentifier());
        if (content == ContentEl.NULL_CONTENT) {
            //skip queries without content
            return;
        }
        final QueryCtxDecorator ctxDecorator = new QueryCtxDecorator(ctx);
        c.executeQuery(content, ctx,
                new QueryCallback() {
                    public void processRow(final ParametersCallback params) {
                        ctxDecorator.setParams(params);

                        for (ExecutableElement exec : nestedElements) {
                            exec.execute(ctxDecorator);
                        }
                    }
                });
    }


    public static ExecutableElement prepare(final QueryEl queryEl) {
        ExecutableElement q = new QueryExecutor(queryEl);
        q = StatisticInterceptor.prepare(q, queryEl.getLocation());
        q = ConnectionInterceptor.prepare(q, queryEl);
        q = ExceptionInterceptor.prepare(q, queryEl.getLocation());
        q = IfInterceptor.prepare(q, queryEl);

        return q;
    }

    private static class QueryCtxDecorator extends DynamicContextDecorator {
        private ParametersCallback params;

        public QueryCtxDecorator(DynamicContext context) {
            super(context);
        }

        void setParams(final ParametersCallback params) {
            this.params = params;
        }

        @Override
        public Object getParameter(final String name) {
            return params.getParameter(name);
        }
    }
}
