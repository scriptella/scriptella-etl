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

import scriptella.configuration.QueryEl;
import scriptella.configuration.ScriptEl;
import scriptella.configuration.ScriptingElement;
import scriptella.spi.Connection;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;

import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * &lt;query&gt; element executor.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public final class QueryExecutor extends ContentExecutor<QueryEl> {
    private static final Object NULL = new Object(); //NULL object flag
    private ExecutableElement[] nestedElements;

    private QueryExecutor(QueryEl queryEl) {
        super(queryEl);
        initNestedExecutors();
    }

    private void initNestedExecutors() {
        final List<ScriptingElement> childElements = getElement().getChildScriptinglElements();
        nestedElements = new ExecutableElement[childElements.size()];

        for (int i = 0; i < nestedElements.length; i++) {
            ScriptingElement element = childElements.get(i);
            if (element instanceof QueryEl) {
                nestedElements[i]=QueryExecutor.prepare((QueryEl) element);
            } else if (element instanceof ScriptEl) {
                nestedElements[i]=ScriptExecutor.prepare((ScriptEl) element);
            } else {
                throw new IllegalStateException("Type " + element.getClass() +
                        " not supported");
            }
        }
    }


    protected void execute(Connection connection, Resource resource, DynamicContext ctx) {
        final QueryCtxDecorator ctxDecorator = new QueryCtxDecorator(ctx);
        if (debug) {
            log.fine("Executing query " + getLocation());
        }
        connection.executeQuery(resource, ctx, ctxDecorator);
        if (debug) {
            if (ctxDecorator.rownum == 0) {
                log.fine("Query " + getLocation() + " returned no results.");
            } else {
                log.fine("Query " + getLocation() + " processed.");
            }

        }

    }


    public static ExecutableElement prepare(final QueryEl queryEl) {
        ExecutableElement q = new QueryExecutor(queryEl);
        q = StatisticInterceptor.prepare(q, queryEl.getLocation());
        q = ConnectionInterceptor.prepare(q, queryEl);
        q = ExceptionInterceptor.prepare(q, queryEl.getLocation());
        q = IfInterceptor.prepare(q, queryEl);

        return q;
    }

    private final class QueryCtxDecorator extends DynamicContextDecorator implements QueryCallback {
        private ParametersCallback params;
        private int rownum; //current row number
        private Map<String, Object> cachedParams;


        public QueryCtxDecorator(DynamicContext context) {
            super(context);
        }


        public void processRow(final ParametersCallback parameters) {
            EtlCancelledException.checkEtlCancelled();
            rownum++;
            params = parameters;
            if (cachedParams != null) {
                cachedParams.clear();
            }
            if (debug) {
                log.fine("Processing row #" + rownum + " for query " + getLocation());
            }

            for (ExecutableElement exec : nestedElements) {
                exec.execute(this);
            }

        }

        @Override
        public final Object getParameter(final String name) {
            if ("rownum".equals(name)) { //return current row number
                return rownum;
            }
            Object res = cachedParams==null?null:cachedParams.get(name);
            if (res == null) {
                res = params.getParameter(name);
                if (res == null) {
                    res = NULL;
                }
                if (isCacheable(res)) {
                    if (cachedParams==null) {
                        cachedParams=new HashMap<String, Object>();
                    }
                    cachedParams.put(name, res);
                }
            }
            return res == NULL ? null : res;
        }

        /**
         * Check if object is cacheable, i.e. no need to fetch it again.
         * @param o object to check.
         * @return true if object is cacheable.
         */
        private boolean isCacheable(Object o) {
            return !(o instanceof InputStream || o instanceof Reader);
        }

    }
}
