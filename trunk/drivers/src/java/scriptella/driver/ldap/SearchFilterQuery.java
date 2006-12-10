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
package scriptella.driver.ldap;

import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an executor for LDAP search filter query(RFC 2254).
 * <p>When {@link #execute(String)} is called a virtual
 * row set based on {@link SearchResult search results} is produced.
 * <p>The {@link javax.naming.directory.SearchResult#getAttributes()} produces
 * columns for a virtual row. The virtual row also contains <code>dn</code> and <code>rdn</code>
 * columns representing a found entry DN and a relative DN respectively.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SearchFilterQuery implements ParametersCallback {
    private static final Logger LOG = Logger.getLogger(SearchFilterQuery.class.getName());
    private QueryCallback queryCallback;

    private SearchResult result;
    private final LdapConnection connection;
    private final PropertiesSubstitutor substitutor;


    /**
     * Instantiates an LDAP query.
     *
     * @param connection    ldap connection.
     * @param parameters    parent parameters callback to get unresolved variables from.
     * @param queryCallback query callback to notify for search results.
     */
    public SearchFilterQuery(final LdapConnection connection, final ParametersCallback parameters,
                             final QueryCallback queryCallback) {

        this.queryCallback = queryCallback;
        this.connection = connection;
        this.substitutor = new PropertiesSubstitutor(parameters);
    }

    public Object getParameter(final String name) {
        final Attributes attributes = result.getAttributes();
        final Attribute attribute = attributes.get(name);
        if (attribute != null) {
            try {
                return attribute.get(); //Currently only the first value is returned
            } catch (NamingException e) {
                throw new LdapProviderException("Failed to get attribute " + name + " value", e);
            }
        } else if ("dn".equalsIgnoreCase(name)) {
            return result.getNameInNamespace(); //JDK14: use getName
        }
        return substitutor.getParameters().getParameter(name);
    }

    /**
     * Runs a search specified by filter on a {@link #connection}.
     * <p>For each search result {@link QueryCallback#processRow(scriptella.spi.ParametersCallback)} is called.
     *
     * @param filter search filter according to RFC 2254
     * @see DirContext#search(javax.naming.Name, String, javax.naming.directory.SearchControls)
     */
    public void execute(final String filter) {
        //Using standard properties substitutor, may be change to something similar to JDBC parameters
        final String sFilter = substitutor.substitute(filter);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Running a query for search filter " + sFilter);
        }
        try {
            iterate(query(connection, sFilter));
        } catch (NamingException e) {
            throw new LdapProviderException("Failed to execute query", e);
        } catch (LdapProviderException e2) {
            //Settings a filter as a poblem statement if it has n't been set.
            if (e2.getErrorStatement() != null) {
                e2.setErrorStatement(sFilter);
            }
            throw e2;
        }
    }

    /**
     * Iterates naming enumeration
     */
    private void iterate(NamingEnumeration<SearchResult> ne) {
        try {
            while (ne.hasMoreElements()) {
                result = ne.nextElement();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Processing search result: " + result);
                }
                queryCallback.processRow(this); //notifying a callback
            }
        } finally {
            try {//closing naming enumeration in case of unexpected error
                ne.close();
            } catch (Exception e) {
                LOG.log(Level.FINE, "Failed to close naming enumeration", e);
            }
        }
    }

    protected NamingEnumeration<SearchResult> query(final LdapConnection connection, final String filter) throws NamingException {
        NamingEnumeration<SearchResult> en = connection.getCtx().search(connection.getBaseDn(), filter, connection.getSearchControls());
        connection.getStatementCounter().statements++;
        return en;
    }


}
