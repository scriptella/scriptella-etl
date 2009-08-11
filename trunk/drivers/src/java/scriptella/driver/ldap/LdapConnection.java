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
package scriptella.driver.ldap;

import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.DriverContext;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.IOUtils;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a connection to a directory context.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class LdapConnection extends AbstractConnection {
    private static final Logger LOG = Logger.getLogger(LdapConnection.class.getName());
    private DirContext ctx;
    private final SearchControls searchControls; //default search controls
    private final Long maxFileLength;
    private final String baseDn;
    private final DriverContext driverContext;


    /**
     * Name of the <em>Search scope</em> connection property.
     * <p>The value must be one of the: object, onelevel, subtree
     *
     * @see SearchControls#setSearchScope(int)
     */
    public static final String SEARCH_SCOPE_KEY = "search.scope";

    /**
     * Name of the <em>Search base DN</em> connection property.
     *
     * @see DirContext#search(String,javax.naming.directory.Attributes)
     */
    public static final String SEARCH_BASEDN_KEY = "search.basedn";


    /**
     * Name of the <em>Time Limit</em> connection property.
     * <p>The value must be integer.
     *
     * @see SearchControls#setTimeLimit(int)
     */
    public static final String SEARCH_TIMELIMIT_KEY = "search.timelimit";

    /**
     * Name of the <em>Count Limit</em>(maximum number of entries to be returned)
     * connection property.
     * <p>The value must be integer.
     *
     * @see SearchControls#setCountLimit(long)
     */
    public static final String SEARCH_COUNTLIMIT_KEY = "search.countlimit";

    /**
     * Names of the Max File Length connection property.
     * <p>This property specifies the maximum size in Kb of the external files referenced from LDIFs.
     * The default value is 10000 (10MB)
     */
    public static final String FILE_MAXLENGTH_KEY = "file.maxlength";

    public LdapConnection() {
        this.searchControls = null;
        this.maxFileLength = null;
        this.baseDn = null;
        this.driverContext = null;
    }

    /**
     * Creates a connnection to a directory.
     *
     * @param parameters parameters to establish connection.
     */
    public LdapConnection(ConnectionParameters parameters) {
        super(Driver.DIALECT, parameters);
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        //Put default settings
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        //Put connection settings
        if (parameters.getUrl() == null) {
            throw new LdapProviderException("Connection URL is required");
        }
        env.put(Context.PROVIDER_URL, parameters.getUrl());
        if (parameters.getUser() != null) {
            env.put(Context.SECURITY_PRINCIPAL, parameters.getUser());
        }
        if (parameters.getPassword() != null) {
            env.put(Context.SECURITY_CREDENTIALS, parameters.getPassword());
        }
        //Override env with user specified connection properties
        env.putAll(parameters.getProperties());
        //Set the search controls used for queries
        searchControls = new SearchControls();
        String scope = parameters.getStringProperty(SEARCH_SCOPE_KEY);
        if (scope != null) {
            if ("object".equalsIgnoreCase(scope)) {
                searchControls.setSearchScope(SearchControls.OBJECT_SCOPE);
            } else if ("onelevel".equalsIgnoreCase(scope)) {
                searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            } else if ("subtree".equalsIgnoreCase(scope)) {
                searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            } else {
                throw new LdapProviderException("Unsupported " + SEARCH_SCOPE_KEY + "=" + scope);
            }
        }
        String baseDn = parameters.getStringProperty(SEARCH_BASEDN_KEY);
        this.baseDn = baseDn == null ? "" : baseDn;

        Integer tl = parameters.getIntegerProperty(SEARCH_TIMELIMIT_KEY);
        if (tl != null) {
            searchControls.setTimeLimit(tl);
        }
        Integer cl = parameters.getIntegerProperty(SEARCH_COUNTLIMIT_KEY);
        if (cl != null) {
            searchControls.setCountLimit(cl);
        }
        Number mfl = parameters.getNumberProperty(FILE_MAXLENGTH_KEY, null);
        maxFileLength = mfl == null ? null : mfl.longValue();

        driverContext = parameters.getContext();
        initializeContext(env); //Initializing context
    }

    /**
     * Creates a directory context.
     *
     * @param env environment to create initial context.
     */
    protected void initializeContext(Hashtable<String, Object> env) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Creating initial context, environment: " + env);
        }
        try {
            ctx = new InitialDirContext(env);
        } catch (NamingException e) {
            throw new LdapProviderException("Unable to establish directory connection", e);
        }
    }

    DirContext getCtx() {
        return ctx;
    }

    SearchControls getSearchControls() {
        return searchControls;
    }

    Long getMaxFileLength() {
        return maxFileLength;
    }

    DriverContext getDriversContext() {
        return driverContext;
    }

    String getBaseDn() {
        return baseDn;
    }

    StatementCounter getStatementCounter() {
        return counter;
    }

    public void executeScript(final Resource scriptContent, final ParametersCallback parametersCallback) throws ProviderException {
        Reader in;
        try {
            in = scriptContent.open();
        } catch (IOException e) {
            throw new LdapProviderException("Failed to read script", e);
        }
        new LdifScript(this).execute(in, ctx, parametersCallback);
    }

    public void executeQuery(final Resource queryContent, final ParametersCallback parametersCallback, final QueryCallback queryCallback) throws ProviderException {
        String filter;
        try {
            filter = IOUtils.toString(queryContent.open()).trim();
        } catch (IOException e) {
            throw new LdapProviderException("Failed to read query filter", e);
        }
        SearchFilterQuery q = new SearchFilterQuery(this, parametersCallback, queryCallback);
        q.execute(filter);
    }

    public void close() throws ProviderException {
        if (ctx != null) {
            try {
                ctx.close();
                ctx = null;
            } catch (NamingException e) {
                throw new LdapProviderException("Unable to close directory context", e);
            }
        }
    }

}
