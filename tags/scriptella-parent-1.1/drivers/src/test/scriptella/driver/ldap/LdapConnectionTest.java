/*
 * Copyright 2006-2012 The Scriptella Project Team.
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
import scriptella.spi.ConnectionParameters;
import scriptella.spi.MockConnectionParameters;

import javax.naming.Context;
import javax.naming.directory.SearchControls;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Tests for {@link LdapConnection}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class LdapConnectionTest extends AbstractTestCase {
    private boolean ctxInitialized;

    /**
     * Tests if LDAP connection correctly initialized.
     */
    public void test() {
        Map<String, String> params = new HashMap<String, String>();
        String dn = "dc=scriptella";
        params.put(LdapConnection.SEARCH_BASEDN_KEY, dn);
        params.put(LdapConnection.SEARCH_SCOPE_KEY, "subtree");
        params.put(LdapConnection.FILE_MAXLENGTH_KEY, "100");
        final String url = "ldap://127.0.0.1:389/";
        ConnectionParameters cp = new MockConnectionParameters(params, url);
        ctxInitialized = false;
        LdapConnection con = new LdapConnection(cp) {
            @Override
            protected void initializeContext(Hashtable<String, Object> env) {
                ctxInitialized = true;
                //Simple checks if environment has been correctly set up
                assertEquals(url, env.get(Context.PROVIDER_URL));
                assertNotNull(env.get(Context.INITIAL_CONTEXT_FACTORY));

            }
        };
        assertEquals(dn, con.getBaseDn());
        assertEquals(SearchControls.SUBTREE_SCOPE, con.getSearchControls().getSearchScope());
        assertEquals(100, (long) con.getMaxFileLength());
        assertTrue(ctxInitialized);
    }
}
