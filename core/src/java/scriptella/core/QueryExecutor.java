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

import scriptella.configuration.ContentEl;
import scriptella.configuration.QueryEl;
import scriptella.configuration.ScriptEl;
import scriptella.configuration.ScriptingElement;
import scriptella.spi.Connection;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class QueryExecutor extends ContentExecutor<QueryEl> {
    private List<ExecutableElement> nestedElements;
    private static final Logger LOG = Logger.getLogger(QueryExecutor.class.getName());

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

        final Resource content = getContent(c.getDialectIdentifier());
        if (content == ContentEl.NULL_CONTENT) {
            //skip queries without content
            return;
        }
        final QueryCtxDecorator ctxDecorator = new QueryCtxDecorator(ctx);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Executing query " + getLocation());
        }
        c.executeQuery(content, ctx,
                new QueryCallback() {
                    public void processRow(final ParametersCallback params) {
                        EtlCancelledException.checkEtlCancelled();
                        ctxDecorator.rownum++;
                        ctxDecorator.setParams(params);
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Processing row #" + ctxDecorator.rownum + " for query " + getLocation());
                        }

                        for (ExecutableElement exec : nestedElements) {
                            exec.execute(ctxDecorator);
                        }
                    }
                });
        if (LOG.isLoggable(Level.FINE)) {
            if (ctxDecorator.rownum == 0) {
                LOG.fine("Query " + getLocation() + " returned no results.");
            } else {
                LOG.fine("Query " + getLocation() + " processed.");
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

    private static final class QueryCtxDecorator extends DynamicContextDecorator {
        private static final Object NULL = new Object(); //NULL object flag
        private ParametersCallback params;
        private int rownum; //current row number
        private Map<String, Object> cachedParams;


        public QueryCtxDecorator(DynamicContext context) {
            super(context);
        }

        void setParams(final ParametersCallback params) {
            this.params = params;
            if (cachedParams != null) {
                cachedParams.clear();
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
