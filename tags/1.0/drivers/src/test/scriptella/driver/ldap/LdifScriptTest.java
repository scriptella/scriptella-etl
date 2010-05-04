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

import scriptella.AbstractTestCase;
import scriptella.driver.ldap.ldif.Entry;
import scriptella.driver.ldap.ldif.LdifReader;
import scriptella.spi.MockParametersCallbacks;
import scriptella.util.ProxyAdapter;

import javax.naming.CompoundName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import java.io.Reader;
import java.io.StringReader;

/**
 * Tests for {@link LdifScript}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class LdifScriptTest extends AbstractTestCase {
    private boolean modified;

    protected void setUp() throws Exception {
        super.setUp();
        modified=false;
    }

    public void testGetRelativeDN() throws NamingException {
        String rootDn = "dc=airius, dc=com";
        String rDn = "ou=PD Accountants, ou=Product Development, dc=airius, dc=com";
        Name actual = LdifScript.getRelativeDN(rootDn, rDn);
        Name expected = newName("ou=PD Accountants, ou=Product Development");
        assertEquals(expected, actual);
        //DirContext is boound to a root
        rootDn = "";
        rDn = "dc=com";
        actual = LdifScript.getRelativeDN(rootDn, rDn);
        expected = newName("dc=com");
        assertEquals(expected, actual);
        //rDn doesn't belong to DirContext
        rootDn = "dc=com";
        rDn = "ou=test";
        try {
            LdifScript.getRelativeDN(rootDn, rDn);
            fail("getRelativeDN works only when rootDn is a part of rDn");
        } catch (NamingException e) {
            //OK
        }
    }

    /**
     * Tests changetype: moddn
     */
    public void testModifyModdn() throws NamingException {
        final Entry e = readEntry(
                "# Rename an entry and move all of its children to a new location in\n" +
                "# the directory tree (only implemented by LDAPv3 servers).\n" +
                "dn: ou=PD Accountants, ou=Product Development, dc=airius, dc=com\n" +
                "changetype: moddn\n" +
                "newrdn: ou=Product Development Accountants\n" +
                "deleteoldrdn: 0\n" +
                "newsuperior: ou=Accounting, dc=airius, dc=com\n");
        DirContext mock = new ProxyAdapter<DirContext>(DirContext.class) {
            //do not delete old rdn
            public void addToEnvironment(String p, Object v) {
                assertEquals("java.naming.ldap.deleteRDN", p);
                assertEquals("false", v);
            }

            public void removeFromEnvironment(String s) {
            }


            public String getNameInNamespace() {
                return "";
            }

            public void rename(Name oldName, Name newName) throws InvalidNameException {
                assertEquals(newName("ou=PD Accountants, ou=Product Development, dc=airius, dc=com"), oldName);
                assertEquals(newName("ou=Product Development Accountants, ou=Accounting, dc=airius, dc=com"), newName);
                modified=true;
            }

        }.getProxy();
        LdifScript.modify(mock, e);
        assertTrue("DirContext was not modified", modified);
    }

    /**
     * Tests changetype: modrdn
     */
    public void testModifyModrdn() throws NamingException {
        final Entry e = readEntry(
                "# Rename an entry and move all of its children to a new location in\n" +
                "# the directory tree (only implemented by LDAPv3 servers).\n" +
                "dn: ou=PD Accountants, dc=com\n" +
                "changetype: modrdn\n" +
                "newrdn: ou=Accountants\n" +
                "deleteoldrdn: 1\n");
        DirContext mock = new ProxyAdapter<DirContext>(DirContext.class) {
            //Delete old rdn
            public void addToEnvironment(String p, Object v) {
                assertEquals("java.naming.ldap.deleteRDN", p);
                assertEquals("true", v);
            }

            public void removeFromEnvironment(String s) {
            }

            public String getNameInNamespace() {
                return "";
            }

            public void rename(Name oldName, Name newName) throws InvalidNameException {
                assertEquals(newName("ou=PD Accountants, dc=com"), oldName);
                assertEquals(newName("ou=Accountants, dc=com"), newName);
                modified=true;
            }

        }.getProxy();
        LdifScript.modify(mock, e);
        assertTrue("DirContext was not modified", modified);
    }

    /**
     * Tests add entry
     *
     */
    public void testModifyAdd() throws NamingException {
        final Entry e = readEntry(
                        "dn: cn=ldap,dc=scriptella\n" +
                        "cn: ldap\n" +
                        "objectClass: top\n" +
                        "objectClass: driver\n" +
                        "envVars:");
        DirContext mock = new ProxyAdapter<DirContext>(DirContext.class) {
            public String getNameInNamespace() {
                return "dc=scriptella";
            }

            public DirContext createSubcontext(Name name, Attributes attrs) throws InvalidNameException {
                assertEquals(newName("cn=ldap"), name);
                BasicAttributes exp = new BasicAttributes(true);
                exp.put("cn", "ldap");
                final BasicAttribute oc = new BasicAttribute("objectClass");
                oc.add("top");
                oc.add("driver");
                exp.put(oc);
                exp.put("envVars", null);
                assertEquals(exp, attrs);
                modified=true;
                return null;
            }

        }.getProxy();
        LdifScript.modify(mock, e);
        assertTrue("DirContext was not modified", modified);
    }

    /**
     * Tests entry removing
     */
    public void testDelete() throws NamingException {
        final Entry e = readEntry(
                        "dn: cn=ldap,dc=scriptella\n" +
                        "changetype: delete\n");
        DirContext mock = new ProxyAdapter<DirContext>(DirContext.class) {
            public String getNameInNamespace() {
                return "dc=scriptella";
            }

            public void destroySubcontext(Name name) throws NamingException {
                assertEquals(newName("cn=ldap"), name);
                modified=true;
            }

        }.getProxy();
        LdifScript.modify(mock, e);
        assertTrue("DirContext was not modified", modified);
    }


    /**
     * Tests changetype: modify
     */
    public void testModify() throws NamingException {
        final Entry e = readEntry(
                "dn: cn=ldap, dc=scriptella\n" +
                "changetype: modify\n" +
                "add: postaladdress\n" +
                "postaladdress: 123 Anystreet\n" +
                "-\n" +
                "delete: description\n" +
                "-\n" +
                "replace: phone\n" +
                "phone: 1234\n" +
                "phone: 5678\n" +
                "-\n" +
                "delete: fax\n" +
                "fax: 1111\n");

        DirContext mock = new ProxyAdapter<DirContext>(DirContext.class) {
            public String getNameInNamespace() {
                return "";
            }
            public void modifyAttributes(Name name, ModificationItem[] mods) throws InvalidNameException {
                assertEquals(newName("cn=ldap, dc=scriptella"), name);
                ModificationItem[] expected = new ModificationItem[4];
                expected[0]=new ModificationItem(DirContext.ADD_ATTRIBUTE,
                        new BasicAttribute("postaladdress", "123 Anystreet"));
                expected[1]=new ModificationItem(DirContext.REMOVE_ATTRIBUTE,
                        new BasicAttribute("description", null));
                BasicAttribute phone = new BasicAttribute("phone");
                phone.add("1234");phone.add("5678");
                expected[2]=new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                        phone);
                expected[3]=new ModificationItem(DirContext.REMOVE_ATTRIBUTE,
                        new BasicAttribute("fax", "1111"));
                for (int i = 0; i < expected.length; i++) {
                    assertEquals(expected[i].getAttribute(), mods[i].getAttribute());
                    assertEquals(expected[i].getModificationOp(), mods[i].getModificationOp());
                }
                modified=true;
            }

        }.getProxy();
        LdifScript.modify(mock, e);
        assertTrue("DirContext was not modified", modified);
    }

    /**
     * Tests if substituted data goes to LDAP.
     */
    public void testExecute() {
        Reader ldif = new StringReader(
                "# Rename an entry and move all of its children to a new location in\n" +
                "# the directory tree (only implemented by LDAPv3 servers).\n" +
                "dn: ou=$test, dc=scriptella\n" +
                "ou: $test\n");

        DirContext mock = new ProxyAdapter<DirContext>(DirContext.class) {
            public String getNameInNamespace() {
                return "dc=scriptella";
            }

            public DirContext createSubcontext(Name name, Attributes attrs) throws InvalidNameException {
                assertEquals(newName("ou=*test*"), name);
                BasicAttributes exp = new BasicAttributes(true);
                exp.put("ou", "*test*");
                assertEquals(exp, attrs);
                modified=true;
                return null;
            }

        }.getProxy();
        LdifScript ls = new LdifScript(new LdapConnection());
        ls.execute(ldif, mock, MockParametersCallbacks.SIMPLE);

        assertTrue("DirContext was not modified", modified);
    }

    /**
     * Tests error handling
     */
    public void testErrorHadnling() throws NamingException {
        String ldif = "dn: cn=ldap,dc=scriptella\n" +
                "changetype: delete\n";
        Reader reader = new StringReader(ldif);
        DirContext mock = new ProxyAdapter<DirContext>(DirContext.class) {
            public String getNameInNamespace() {
                return "dc=scriptella";
            }

            public void destroySubcontext(Name name) throws NamingException {
                throw new NamingException("Failure");
            }
        }.getProxy();
        try {
            LdifScript ls = new LdifScript(new LdapConnection());
            ls.execute(reader, mock, MockParametersCallbacks.UNSUPPORTED);

        } catch (LdapProviderException e) {
            Throwable ne = e.getNativeException();
            assertEquals(NamingException.class, ne.getClass());
            assertEquals("Failure", ne.getMessage());
            assertEquals(ldif, e.getErrorStatement());
        }
    }


    /**
     * @param s ldif
     * @return entry from ldif
     */
    private static Entry readEntry(String s) {
        LdifReader lr = new LdifReader(s);
        return lr.next();
    }

    private static Name newName(String name) throws InvalidNameException {
        return new CompoundName(name, LdifScript.DN_SYNTAX);
    }
}
