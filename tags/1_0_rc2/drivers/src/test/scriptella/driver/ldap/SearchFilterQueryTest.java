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
package scriptella.driver.ldap;

import scriptella.AbstractTestCase;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.util.ProxyAdapter;

import javax.naming.NamingEnumeration;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tests for {@link SearchFilterQuery}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SearchFilterQueryTest extends AbstractTestCase {
    private boolean closed;
    private int rows;

    protected void setUp() throws Exception {
        super.setUp();
        closed=false;
        rows=0;
    }

    public void testExecute() {
        QueryCallback qc = new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                assertEquals("uid"+rows, parameters.getParameter("uid"));
                assertEquals("search"+rows, parameters.getParameter("cn"));
                assertEquals("cn=search"+rows+", ou=ldap, dc=scriptella", parameters.getParameter("dn"));
                rows++;
            }
        };

        SearchFilterQuery q = new SearchFilterQuery(null, MockParametersCallbacks.UNSUPPORTED, qc) {
            protected NamingEnumeration<SearchResult> query(final LdapConnection connection, final String filter) {
                List<SearchResult> res = new ArrayList<SearchResult>();
                for (int i=0;i<2;i++) {
                    BasicAttributes a = new BasicAttributes("uid","uid"+i);
                    a.put("cn", "search"+i);
                    SearchResult sr = new SearchResult("cn=search"+i+", ou=ldap, dc=scriptella", null, a);
                    sr.setNameInNamespace(sr.getName());
                    res.add(sr);
                }
                final Iterator<SearchResult> it = res.iterator();
                return new NamingEnumeration<SearchResult>() {

                    public SearchResult next() {
                        return it.next();
                    }

                    public boolean hasMore() {
                        return it.hasNext();
                    }

                    public void close() {
                        closed=true;
                    }

                    public boolean hasMoreElements() {
                        return hasMore();
                    }

                    public SearchResult nextElement() {
                        return next();
                    }
                };
            }
        };
        q.execute("filter");//in this test case filter doesn't matter
        assertTrue("Naming enumeration must be closed after iteration", closed);
        assertEquals(2, rows);
    }

    /**
     * Tests if variables are substituted in a passed filter.
     */
    public void testExecuteSubstitution() {
        SearchFilterQuery q = new SearchFilterQuery(null, MockParametersCallbacks.SIMPLE, null) {
            protected NamingEnumeration<SearchResult> query(final LdapConnection connection, final String filter) {
                assertEquals("test *filter* *a**b*", filter);
                return new ProxyAdapter<NamingEnumeration<SearchResult>>(NamingEnumeration.class) {
                    public boolean hasMoreElements() {
                        return false;
                    }
                }.getProxy();

            }
        };
        q.execute("test $filter ${a+b}");
    }
}
