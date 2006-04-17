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

import scriptella.configuration.ContentEl;
import scriptella.configuration.QueryEl;
import scriptella.configuration.SQLBasedElement;
import scriptella.configuration.ScriptEl;
import scriptella.expressions.ParametersCallback;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class QueryExecutor implements SQLExecutableElement {
    private QueryEl queryEl;
    private List<SQLExecutableElement> nestedElements;

    private QueryExecutor(QueryEl queryEl) {
        this.queryEl = queryEl;
        initNestedExecutors();
    }

    private void initNestedExecutors() {
        final List<SQLBasedElement> childSqlElements = queryEl.getChildSqlElements();
        nestedElements = new ArrayList<SQLExecutableElement>(childSqlElements.size());

        for (SQLBasedElement element : childSqlElements) {
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

    public void execute(final SQLContext ctx) {
        final Query query = convert(queryEl, ctx);

        if (query == null) {
            return; //we should not execute queries without content
        }

        final Connection c = ctx.getConnection();
        final QueryCtxDecorator ctxDecorator = new QueryCtxDecorator(ctx);
        query.execute(c, ctx,
                new QueryCallback() {
                    public void processRow(final ParametersCallback params) {
                        ctxDecorator.setParams(params);

                        for (SQLExecutableElement exec : nestedElements) {
                            exec.execute(ctxDecorator);
                        }
                    }
                });
    }

    private Query convert(final QueryEl q, final SQLContext ctx) {
        final ContentEl content = q.getContent(ctx.getDialectIdentifier());

        if (content == null) {
            return null;
        } else {
            return new Query(content);
        }
    }

    public static SQLExecutableElement prepare(final QueryEl queryEl) {
        SQLExecutableElement q = new QueryExecutor(queryEl);
        q = StatisticInterceptor.prepare(q, queryEl.getLocation());
        q = ConnectionInterceptor.prepare(q, queryEl);
        q = ExceptionInterceptor.prepare(q, queryEl.getLocation());
        q = IfInterceptor.prepare(q, queryEl);

        return q;
    }

    private static class QueryCtxDecorator extends SQLContextDecorator {
        private ParametersCallback params;

        public QueryCtxDecorator(SQLContext context) {
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
