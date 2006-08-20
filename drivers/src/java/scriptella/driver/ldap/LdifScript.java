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

import scriptella.driver.ldap.ldif.Entry;
import scriptella.driver.ldap.ldif.LdifParseException;
import scriptella.driver.ldap.ldif.LdifReader;
import scriptella.driver.ldap.ldif.SubstitutingLineReader;
import scriptella.spi.ParametersCallback;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executor for LDIF script.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class LdifScript {
    private static final Logger LOG = Logger.getLogger(LdifScript.class.getName());
    //Parsing options for DN
    static final Properties DN_SYNTAX = new Properties();

    static {
        DN_SYNTAX.setProperty("jndi.syntax.direction", "right_to_left");
        DN_SYNTAX.setProperty("jndi.syntax.separator", ",");
        DN_SYNTAX.setProperty("jndi.syntax.ignorecase", "true");
        DN_SYNTAX.setProperty("jndi.syntax.trimblanks", "true");
    }

    private final LdapConnection connection;

    public LdifScript(LdapConnection connection) {
        this.connection = connection;
    }

    /**
     * Executes an LDIF content from the specified reader.
     *
     * @param reader reader with LDIF content.
     * @throws LdapProviderException if directory access failed.
     */
    public void execute(Reader reader, DirContext ctx, ParametersCallback parameters) throws LdapProviderException {
        SubstitutingLineReader in = new SubstitutingLineReader(reader, parameters);
        try {
            in.trackLines();
            LdifIterator it = new LdifIterator(reader);
            while (it.hasNext()) {
                in.trackLines();
                Entry e = it.next();
                modify(ctx, e);
            }
        } catch (LdifParseException e) {
            if (e.getErrorStatement()==null) {
                e.setErrorStatement(in.getTrackedLines());
            }
            throw e;
        } catch (NamingException e) {
            LdapProviderException ex = new LdapProviderException("Failed to execute LDIF entry", e);
            ex.setErrorStatement(in.getTrackedLines());
            throw ex;
        }
    }

    /**
     * Adds/modifies ctx using entry information.
     *
     * @param ctx directory context to use for change.
     * @param e   entry with change description.
     * @throws NamingException if operation with directory failed.
     */
    static void modify(DirContext ctx, final Entry e) throws NamingException {
        //todo add support for renaming
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Processing entry " + e);
        }
        Attributes atts = e.getAttributes();
        final String rootDn = ctx.getNameInNamespace();
        if (atts != null) { //If add entry
            ctx.createSubcontext(getRelativeDN(rootDn, e.getDn()), e.getAttributes());
        } else if (e.isChangeModDn() || e.isChangeModRdn()) {
            Name newRdn;
            if (e.getNewSuperior()!=null) { //If new superior
                newRdn=getRelativeDN(rootDn, e.getNewSuperior());
            } else { //otherwise use DN as a base
                newRdn=getRelativeDN(rootDn, e.getDn());
                newRdn.remove(newRdn.size()-1);
            }
            newRdn.add(e.getNewRdn());
            ctx.addToEnvironment("java.naming.ldap.deleteRDN", String.valueOf(e.isDeleteOldRdn()));
            ctx.rename(getRelativeDN(rootDn, e.getDn()), newRdn);
            ctx.removeFromEnvironment("java.naming.ldap.deleteRDN");//a better solution to use the previous value

        } else {
            List<ModificationItem> items = e.getModificationItems();
            ctx.modifyAttributes(getRelativeDN(rootDn, e.getDn()),
                    items.toArray(new ModificationItem[items.size()]));
        }
    }


    /**
     * @param rootDn root context DN.
     * @param dn     DN to compute a relative name. DN must starts with rootDn.
     * @return name relative to a root context DN.
     */
    static Name getRelativeDN(final String rootDn, final String dn) throws NamingException {
        CompoundName root = new CompoundName(rootDn, DN_SYNTAX);
        CompoundName entry = new CompoundName(dn, DN_SYNTAX);
        if (!entry.startsWith(root)) {
            throw new NamingException("Dn " + dn + " is not from root DN " + rootDn);
        }
        return entry.getSuffix(root.size());
    }

    private class LdifIterator extends LdifReader {

        public LdifIterator(Reader in) {
            super(in);
            if (connection.getMaxFileLength()!=null) {
                setSizeLimit(connection.getMaxFileLength()*1024);
            }
        }

        protected InputStream getUriStream(String uri) throws IOException {
            return connection.getDriversContext().resolve(uri).openStream();
        }
    }

}
