/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package scriptella.driver.ldap.ldif;

import junit.framework.TestCase;
import scriptella.expression.LineIterator;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.Control;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link LdifReader}.
 * Based on LdifReaderTest from Apache Driectory Project.
 *
 * @author <a href="mailto:akarasulu@safehaus.org">Alex Karasulu</a>
 * @author Fyodor Kupolov
 * @version $Rev: 379008 $
 */
public class LdifReaderTest extends TestCase {
    private byte[] data;

    private static File HJENSEN_JPEG_FILE = null;
    private static File FIONA_JPEG_FILE = null;

    private File createFile(String name, byte[] data) throws IOException {
        File jpeg = File.createTempFile(name, "jpg");

        jpeg.createNewFile();

        DataOutputStream os = new DataOutputStream(new FileOutputStream(jpeg));

        os.write(data);
        os.close();

        // This file will be deleted when the JVM
        // will exit.
        jpeg.deleteOnExit();

        return jpeg;
    }

    /**
     * Create a file to be used by ":<" values
     */
    public void setUp() throws Exception {
        super.setUp();

        data = new byte[256];

        for (int i = 0; i < 256; i++) {
            data[i] = (byte) i;
        }

        HJENSEN_JPEG_FILE = createFile("hjensen", data);
        FIONA_JPEG_FILE = createFile("fiona", data);
    }

    public void testLdifEmpty() throws NamingException {
        String ldif = "";

        List entries = new LdifReader(ldif).asList();

        assertEquals(0, entries.size());
    }

    public void testLdifEmptyLines() throws NamingException {
        String ldif = "\n\n\r\r\n";

        List entries = new LdifReader(ldif).asList();

        assertEquals(0, entries.size());
    }

    public void testLdifComments() throws NamingException {
        String ldif =
                "#Comment 1\r" +
                        "#\r" +
                        " th\n" +
                        " is is still a comment\n" +
                        "\n";

        List entries = new LdifReader(ldif).asList();

        assertEquals(0, entries.size());
    }

    public void testLdifVersion() throws NamingException {
        String ldif =
                "#Comment 1\r" +
                "#\r" +
                " th\n" +
                " is is still a comment\n" +
                "\n" +
                "version:\n" +
                " 2\n" +
                "# end";

        LdifReader reader = new LdifReader(ldif);
        reader.asList();
        List entries = reader.asList();

        assertEquals(0, entries.size());
        assertEquals(2, reader.getVersion());
    }

    /**
     * Spaces at the end of values should not be included into values.
     *
     * @throws NamingException
     */
    public void testLdifParserEndSpaces() throws NamingException {
        String ldif =
                "version:   1\n" +
                        "dn: cn=app1,ou=applications,ou=conf,dc=apache,dc=org\n" +
                        "cn: app1\n" +
                        "objectClass: top\n" +
                        "objectClass: apApplication\n" +
                        "displayName:   app1   \n" +
                        "dependencies:\n" +
                        "envVars:";

        List entries = new LdifReader(ldif).asList();
        assertNotNull(entries);

        Entry entry = (Entry) entries.get(0);

        assertTrue(entry.isChangeAdd());

        assertEquals("cn=app1,ou=applications,ou=conf,dc=apache,dc=org", entry.getDn());

        Attribute attr = entry.get("displayname");
        assertTrue(attr.contains("app1"));

    }

    /**
     * Changes and entries should not be mixed
     *
     * @throws NamingException
     */
    public void testLdifParserCombinedEntriesChanges() throws NamingException {
        String ldif =
                "version:   1\n" +
                        "dn: cn=app1,ou=applications,ou=conf,dc=apache,dc=org\n" +
                        "cn: app1\n" +
                        "objectClass: top\n" +
                        "objectClass: apApplication\n" +
                        "displayName:   app1   \n" +
                        "dependencies:\n" +
                        "envVars:\n" +
                        "\n" +
                        "# Delete an entry. The operation will attach the LDAPv3\n" +
                        "# Tree Delete Control defined in [9]. The criticality\n" +
                        "# field is \"true\" and the controlValue field is\n" +
                        "# absent, as required by [9].\n" +
                        "dn: ou=Product Development, dc=airius, dc=com\n" +
                        "control: 1.2.840.11A556.1.4.805 true\n" +
                        "changetype: delete\n";

        try {
            new LdifReader(ldif).asList();
            fail();
        }
        catch (LdifParseException e) {
            //OK
        }
    }

    /**
     * Changes and entries should not be mixed
     *
     * @throws NamingException
     */
    public void testLdifParserCombinedEntriesChanges2() throws NamingException {
        String ldif =
                "version:   1\n" +
                        "dn: cn=app1,ou=applications,ou=conf,dc=apache,dc=org\n" +
                        "cn: app1\n" +
                        "objectClass: top\n" +
                        "objectClass: apApplication\n" +
                        "displayName:   app1   \n" +
                        "dependencies:\n" +
                        "envVars:\n" +
                        "\n" +
                        "# Delete an entry. The operation will attach the LDAPv3\n" +
                        "# Tree Delete Control defined in [9]. The criticality\n" +
                        "# field is \"true\" and the controlValue field is\n" +
                        "# absent, as required by [9].\n" +
                        "dn: ou=Product Development, dc=airius, dc=com\n" +
                        "changetype: delete\n";

        try {
            new LdifReader(ldif).asList();
            fail();
        }
        catch (LdifParseException e) {
            //OK
        }
    }

    /**
     * Changes and entries should not be mixed
     *
     * @throws NamingException
     */
    public void testLdifParserCombinedChangesEntries() throws NamingException {
        String ldif =
                "version:   1\n" +
                        "# Delete an entry. The operation will attach the LDAPv3\n" +
                        "# Tree Delete Control defined in [9]. The criticality\n" +
                        "# field is \"true\" and the controlValue field is\n" +
                        "# absent, as required by [9].\n" +
                        "dn: ou=Product Development, dc=airius, dc=com\n" +
                        "control: 1.2.840.11A556.1.4.805 true\n" +
                        "changetype: delete\n" +
                        "\n" +
                        "dn: cn=app1,ou=applications,ou=conf,dc=apache,dc=org\n" +
                        "cn: app1\n" +
                        "objectClass: top\n" +
                        "objectClass: apApplication\n" +
                        "displayName:   app1   \n" +
                        "dependencies:\n" +
                        "envVars:\n";

        try {
            new LdifReader(ldif).asList();
            fail();
        }
        catch (LdifParseException e) {
            //OK
        }
    }

    /**
     * Changes and entries should not be mixed
     *
     * @throws NamingException
     */
    public void testLdifParserCombinedChangesEntries2() throws NamingException {
        String ldif =
                "version:   1\n" +
                        "# Delete an entry. The operation will attach the LDAPv3\n" +
                        "# Tree Delete Control defined in [9]. The criticality\n" +
                        "# field is \"true\" and the controlValue field is\n" +
                        "# absent, as required by [9].\n" +
                        "dn: ou=Product Development, dc=airius, dc=com\n" +
                        "changetype: delete\n" +
                        "\n" +
                        "dn: cn=app1,ou=applications,ou=conf,dc=apache,dc=org\n" +
                        "cn: app1\n" +
                        "objectClass: top\n" +
                        "objectClass: apApplication\n" +
                        "displayName:   app1   \n" +
                        "dependencies:\n" +
                        "envVars:\n";

        try {
            new LdifReader(ldif).asList();
            fail();
        }
        catch (LdifParseException e) {
            //OK
        }
    }

    public void testLdifParser() throws NamingException {
        String ldif =
                "version:   1\n" +
                        "dn: cn=app1,ou=applications,ou=conf,dc=apache,dc=org\n" +
                        "cn: app1\n" +
                        "objectClass: top\n" +
                        "objectClass: apApplication\n" +
                        "displayName: app1   \n" +
                        "dependencies:\n" +
                        "envVars:";

        List entries = new LdifReader(ldif).asList();

        assertNotNull(entries);

        Entry entry = (Entry) entries.get(0);
        assertTrue(entry.isChangeAdd());

        assertEquals("cn=app1,ou=applications,ou=conf,dc=apache,dc=org", entry.getDn());

        Attribute attr = entry.get("cn");
        assertTrue(attr.contains("app1"));

        attr = entry.get("objectclass");
        assertTrue(attr.contains("top"));
        assertTrue(attr.contains("apApplication"));

        attr = entry.get("displayname");
        assertTrue(attr.contains("app1"));

        attr = entry.get("dependencies");
        assertNull(attr.get());

        attr = entry.get("envvars");
        assertNull(attr.get());
    }

    public void testLdifParserMuiltiLineComments() throws NamingException {
        String ldif =
                "#comment\n" +
                        " still a comment\n" +
                        "dn: cn=app1,ou=applications,ou=conf,dc=apache,dc=org\n" +
                        "cn: app1#another comment\n" +
                        "objectClass: top\n" +
                        "objectClass: apApplication\n" +
                        "displayName: app1\n" +
                        "serviceType: http\n" +
                        "dependencies:\n" +
                        "httpHeaders:\n" +
                        "startupOptions:\n" +
                        "envVars:";

        List entries = new LdifReader(ldif).asList();

        assertNotNull(entries);

        Entry entry = (Entry) entries.get(0);
        assertTrue(entry.isChangeAdd());

        assertEquals("cn=app1,ou=applications,ou=conf,dc=apache,dc=org", entry.getDn());

        Attribute attr = entry.get("cn");
        assertTrue(attr.contains("app1#another comment"));

        attr = entry.get("objectclass");
        assertTrue(attr.contains("top"));
        assertTrue(attr.contains("apApplication"));

        attr = entry.get("displayname");
        assertTrue(attr.contains("app1"));

        attr = entry.get("dependencies");
        assertNull(attr.get());

        attr = entry.get("envvars");
        assertNull(attr.get());
    }

    public void testLdifParserMultiLineEntries() throws NamingException {
        String ldif =
                "#comment\n" +
                        "dn: cn=app1,ou=appli\n" +
                        " cations,ou=conf,dc=apache,dc=org\n" +
                        "cn: app1#another comment\n" +
                        "objectClass: top\n" +
                        "objectClass: apApplication\n" +
                        "displayName: app1\n" +
                        "serviceType: http\n" +
                        "dependencies:\n" +
                        "httpHeaders:\n" +
                        "startupOptions:\n" +
                        "envVars:";

        List entries = new LdifReader(ldif).asList();

        assertNotNull(entries);

        Entry entry = (Entry) entries.get(0);
        assertTrue(entry.isChangeAdd());

        assertEquals("cn=app1,ou=applications,ou=conf,dc=apache,dc=org", entry.getDn());

        Attribute attr = entry.get("cn");
        assertTrue(attr.contains("app1#another comment"));

        attr = entry.get("objectclass");
        assertTrue(attr.contains("top"));
        assertTrue(attr.contains("apApplication"));

        attr = entry.get("displayname");
        assertTrue(attr.contains("app1"));

        attr = entry.get("dependencies");
        assertNull(attr.get());

        attr = entry.get("envvars");
        assertNull(attr.get());
    }

    public void testLdifParserBase64() throws NamingException, UnsupportedEncodingException {
        String ldif =
                "#comment\n" +
                        "dn: cn=app1,ou=applications,ou=conf,dc=apache,dc=org\n" +
                        "cn:: RW1tYW51ZWwgTMOpY2hhcm55\n" +
                        "objectClass: top\n" +
                        "objectClass: apApplication\n" +
                        "displayName: app1\n" +
                        "serviceType: http\n" +
                        "dependencies:\n" +
                        "httpHeaders:\n" +
                        "startupOptions:\n" +
                        "envVars:";

        List entries = new LdifReader(ldif).asList();

        assertNotNull(entries);

        Entry entry = (Entry) entries.get(0);
        assertTrue(entry.isChangeAdd());

        assertEquals("cn=app1,ou=applications,ou=conf,dc=apache,dc=org", entry.getDn());

        Attribute attr = entry.get("cn");
        assertTrue(attr.contains("Emmanuel L\u00e9charny".getBytes("UTF-8")));

        attr = entry.get("objectclass");
        assertTrue(attr.contains("top"));
        assertTrue(attr.contains("apApplication"));

        attr = entry.get("displayname");
        assertTrue(attr.contains("app1"));

        attr = entry.get("dependencies");
        assertNull(attr.get());

        attr = entry.get("envvars");
        assertNull(attr.get());
    }

    public void testLdifParserBase64MultiLine() throws NamingException, UnsupportedEncodingException {
        String ldif =
                "#comment\n" +
                        "dn: cn=app1,ou=applications,ou=conf,dc=apache,dc=org\n" +
                        "cn:: RW1tYW51ZWwg\n" +
                        " TMOpY2hhcm55ICA=\n" +
                        "objectClass: top\n" +
                        "objectClass: apApplication\n" +
                        "displayName: app1\n" +
                        "serviceType: http\n" +
                        "dependencies:\n" +
                        "httpHeaders:\n" +
                        "startupOptions:\n" +
                        "envVars:";

        List entries = new LdifReader(ldif).asList();

        assertNotNull(entries);

        Entry entry = (Entry) entries.get(0);
        assertTrue(entry.isChangeAdd());

        assertEquals("cn=app1,ou=applications,ou=conf,dc=apache,dc=org", entry.getDn());

        Attribute attr = entry.get("cn");
        assertTrue(attr.contains("Emmanuel L\u00e9charny  ".getBytes("UTF-8")));

        attr = entry.get("objectclass");
        assertTrue(attr.contains("top"));
        assertTrue(attr.contains("apApplication"));

        attr = entry.get("displayname");
        assertTrue(attr.contains("app1"));

        attr = entry.get("dependencies");
        assertNull(attr.get());

        attr = entry.get("envvars");
        assertNull(attr.get());
    }

    public void testLdifParserRFC2849Sample1() throws NamingException {
        String ldif =
                "version: 1\n" +
                        "dn: cn=Barbara Jensen, ou=Product Development, dc=airius, dc=com\n" +
                        "objectclass: top\n" +
                        "objectclass: person\n" +
                        "objectclass: organizationalPerson\n" +
                        "cn: Barbara Jensen\n" +
                        "cn: Barbara J Jensen\n" +
                        "cn: Babs Jensen\n" +
                        "sn: Jensen\n" +
                        "uid: bjensen\n" +
                        "telephonenumber: +1 408 555 1212\n" +
                        "description: A big sailing fan.\n" +
                        "\n" +
                        "dn: cn=Bjorn Jensen, ou=Accounting, dc=airius, dc=com\n" +
                        "objectclass: top\n" +
                        "objectclass: person\n" +
                        "objectclass: organizationalPerson\n" +
                        "cn: Bjorn Jensen\n" +
                        "sn: Jensen\n" +
                        "telephonenumber: +1 408 555 1212";

        List entries = new LdifReader(ldif).asList();

        assertEquals(2, entries.size());

        // Entry 1
        Entry entry = (Entry) entries.get(0);
        assertTrue(entry.isChangeAdd());

        assertEquals("cn=Barbara Jensen, ou=Product Development, dc=airius, dc=com", entry.getDn());

        Attribute attr = entry.get("objectclass");
        assertTrue(attr.contains("top"));
        assertTrue(attr.contains("person"));
        assertTrue(attr.contains("organizationalPerson"));

        attr = entry.get("cn");
        assertTrue(attr.contains("Barbara Jensen"));
        assertTrue(attr.contains("Barbara J Jensen"));
        assertTrue(attr.contains("Babs Jensen"));

        attr = entry.get("sn");
        assertTrue(attr.contains("Jensen"));

        attr = entry.get("uid");
        assertTrue(attr.contains("bjensen"));

        attr = entry.get("telephonenumber");
        assertTrue(attr.contains("+1 408 555 1212"));

        attr = entry.get("description");
        assertTrue(attr.contains("A big sailing fan."));

        // Entry 2
        entry = (Entry) entries.get(1);
        assertTrue(entry.isChangeAdd());

        assertEquals("cn=Bjorn Jensen, ou=Accounting, dc=airius, dc=com", entry.getDn());

        attr = entry.get("objectclass");
        assertTrue(attr.contains("top"));
        assertTrue(attr.contains("person"));
        assertTrue(attr.contains("organizationalPerson"));

        attr = entry.get("cn");
        assertTrue(attr.contains("Bjorn Jensen"));

        attr = entry.get("sn");
        assertTrue(attr.contains("Jensen"));

        attr = entry.get("telephonenumber");
        assertTrue(attr.contains("+1 408 555 1212"));
    }

    public void testLdifParserRFC2849Sample2() throws NamingException {
        String ldif =
                "version: 1\n" +
                        "dn: cn=Barbara Jensen, ou=Product Development, dc=airius, dc=com\n" +
                        "objectclass: top\n" +
                        "objectclass: person\n" +
                        "objectclass: organizationalPerson\n" +
                        "cn: Barbara Jensen\n" +
                        "cn: Barbara J Jensen\n" +
                        "cn: Babs Jensen\n" +
                        "sn: Jensen\n" +
                        "uid: bjensen\n" +
                        "telephonenumber: +1 408 555 1212\n" +
                        "description:Babs is a big sailing fan, and travels extensively in sea\n" +
                        " rch of perfect sailing conditions.\n" +
                        "title:Product Manager, Rod and Reel Division";

        List entries = new LdifReader(ldif).asList();

        assertEquals(1, entries.size());

        // Entry 1
        Entry entry = (Entry) entries.get(0);
        assertTrue(entry.isChangeAdd());

        assertEquals("cn=Barbara Jensen, ou=Product Development, dc=airius, dc=com", entry.getDn());

        Attribute attr = entry.get("objectclass");
        assertTrue(attr.contains("top"));
        assertTrue(attr.contains("person"));
        assertTrue(attr.contains("organizationalPerson"));

        attr = entry.get("cn");
        assertTrue(attr.contains("Barbara Jensen"));
        assertTrue(attr.contains("Barbara J Jensen"));
        assertTrue(attr.contains("Babs Jensen"));

        attr = entry.get("sn");
        assertTrue(attr.contains("Jensen"));

        attr = entry.get("uid");
        assertTrue(attr.contains("bjensen"));

        attr = entry.get("telephonenumber");
        assertTrue(attr.contains("+1 408 555 1212"));

        attr = entry.get("description");
        assertTrue(attr
                .contains("Babs is a big sailing fan, and travels extensively in search of perfect sailing conditions."));

        attr = entry.get("title");
        assertTrue(attr.contains("Product Manager, Rod and Reel Division"));

    }

    public void testLdifParserRFC2849Sample3() throws NamingException, Exception {
        String ldif =
                "version: 1\n" +
                        "dn: cn=Gern Jensen, ou=Product Testing, dc=airius, dc=com\n" +
                        "objectclass: top\n" +
                        "objectclass: person\n" +
                        "objectclass: organizationalPerson\n" +
                        "cn: Gern Jensen\n" +
                        "cn: Gern O Jensen\n" +
                        "sn: Jensen\n" +
                        "uid: gernj\n" +
                        "telephonenumber: +1 408 555 1212\n" +
                        "description:: V2hhdCBhIGNhcmVmdWwgcmVhZGVyIHlvdSBhcmUhICBUaGlzIHZhbHVl\n" +
                        " IGlzIGJhc2UtNjQtZW5jb2RlZCBiZWNhdXNlIGl0IGhhcyBhIGNvbnRyb2wgY2hhcmFjdG\n" +
                        " VyIGluIGl0IChhIENSKS4NICBCeSB0aGUgd2F5LCB5b3Ugc2hvdWxkIHJlYWxseSBnZXQg\n" +
                        " b3V0IG1vcmUu";

        List entries = new LdifReader(ldif).asList();

        assertEquals(1, entries.size());

        // Entry 1
        Entry entry = (Entry) entries.get(0);
        assertTrue(entry.isChangeAdd());

        assertEquals("cn=Gern Jensen, ou=Product Testing, dc=airius, dc=com", entry.getDn());

        Attribute attr = entry.get("objectclass");
        assertTrue(attr.contains("top"));
        assertTrue(attr.contains("person"));
        assertTrue(attr.contains("organizationalPerson"));

        attr = entry.get("cn");
        assertTrue(attr.contains("Gern Jensen"));
        assertTrue(attr.contains("Gern O Jensen"));

        attr = entry.get("sn");
        assertTrue(attr.contains("Jensen"));

        attr = entry.get("uid");
        assertTrue(attr.contains("gernj"));

        attr = entry.get("telephonenumber");
        assertTrue(attr.contains("+1 408 555 1212"));

        attr = entry.get("description");
        assertTrue(attr
                .contains("What a careful reader you are!  This value is base-64-encoded because it has a control character in it (a CR).\r  By the way, you should really get out more."
                .getBytes("UTF-8")));
    }

    public void testLdifParserRFC2849Sample3VariousSpacing() throws NamingException, Exception {
        String ldif =
                "version:1\n" +
                        "dn:cn=Gern Jensen, ou=Product Testing, dc=airius, dc=com  \n" +
                        "objectclass:top\n" +
                        "objectclass:   person   \n" +
                        "objectclass:organizationalPerson\n" +
                        "cn:Gern Jensen\n" +
                        "cn:Gern O Jensen\n" +
                        "sn:Jensen\n" +
                        "uid:gernj\n" +
                        "telephonenumber:+1 408 555 1212  \n" +
                        "description::  V2hhdCBhIGNhcmVmdWwgcmVhZGVyIHlvdSBhcmUhICBUaGlzIHZhbHVl\n" +
                        " IGlzIGJhc2UtNjQtZW5jb2RlZCBiZWNhdXNlIGl0IGhhcyBhIGNvbnRyb2wgY2hhcmFjdG\n" +
                        " VyIGluIGl0IChhIENSKS4NICBCeSB0aGUgd2F5LCB5b3Ugc2hvdWxkIHJlYWxseSBnZXQg\n" +
                        " b3V0IG1vcmUu  ";

        List entries = new LdifReader(ldif).asList();

        assertEquals(1, entries.size());

        // Entry 1
        Entry entry = (Entry) entries.get(0);
        assertTrue(entry.isChangeAdd());

        assertEquals("cn=Gern Jensen, ou=Product Testing, dc=airius, dc=com", entry.getDn());

        Attribute attr = entry.get("objectclass");
        assertTrue(attr.contains("top"));
        assertTrue(attr.contains("person"));
        assertTrue(attr.contains("organizationalPerson"));

        attr = entry.get("cn");
        assertTrue(attr.contains("Gern Jensen"));
        assertTrue(attr.contains("Gern O Jensen"));

        attr = entry.get("sn");
        assertTrue(attr.contains("Jensen"));

        attr = entry.get("uid");
        assertTrue(attr.contains("gernj"));

        attr = entry.get("telephonenumber");
        assertTrue(attr.contains("+1 408 555 1212"));

        attr = entry.get("description");
        assertTrue(attr
                .contains("What a careful reader you are!  This value is base-64-encoded because it has a control character in it (a CR).\r  By the way, you should really get out more."
                .getBytes("UTF-8")));
    }

    public void testLdifParserRFC2849Sample4() throws NamingException, Exception {
        String ldif =
                "version: 1\n" +
                        "dn:: b3U95Za25qWt6YOoLG89QWlyaXVz\n" +
                        "# dn:: ou=��?������,o=Airius\n" +
                        "objectclass: top\n" +
                        "objectclass: organizationalUnit\n" +
                        "ou:: 5Za25qWt6YOo\n" +
                        "# ou:: ��?������\n" +
                        "ou;lang-ja:: 5Za25qWt6YOo\n" +
                        "# ou;lang-ja:: ��?������\n" +
                        "ou;lang-ja;phonetic:: 44GI44GE44GO44KH44GG44G2\n" +
                        "# ou;lang-ja:: �����������������?\n" +
                        "ou;lang-en: Sales\n" +
                        "description: Japanese office\n" +
                        "\n" +
                        "dn:: dWlkPXJvZ2FzYXdhcmEsb3U95Za25qWt6YOoLG89QWlyaXVz\n" +
                        "# dn:: uid=rogasawara,ou=��?������,o=Airius\n" +
                        "userpassword: {SHA}O3HSv1MusyL4kTjP+HKI5uxuNoM=\n" +
                        "objectclass: top\n" +
                        "objectclass: person\n" +
                        "objectclass: organizationalPerson\n" +
                        "objectclass: inetOrgPerson\n" +
                        "uid: rogasawara\n" +
                        "mail: rogasawara@airius.co.jp\n" +
                        "givenname;lang-ja:: 44Ot44OJ44OL44O8\n" +
                        "# givenname;lang-ja:: �����������?\n" +
                        "sn;lang-ja:: 5bCP56yg5Y6f\n" +
                        "# sn;lang-ja:: �?�������\n" +
                        "cn;lang-ja:: 5bCP56yg5Y6fIOODreODieODi+ODvA==\n" +
                        "# cn;lang-ja:: �?������� �����������?\n" +
                        "title;lang-ja:: 5Za25qWt6YOoIOmDqOmVtw==\n" +
                        "# title;lang-ja:: ��?������ �����?\n" +
                        "preferredlanguage: ja\n" +
                        "givenname:: 44Ot44OJ44OL44O8\n" +
                        "# givenname:: �����������?\n" +
                        "sn:: 5bCP56yg5Y6f\n" +
                        "# sn:: �?�������\n" +
                        "cn:: 5bCP56yg5Y6fIOODreODieODi+ODvA==\n" +
                        "# cn:: �?������� �����������?\n" +
                        "title:: 5Za25qWt6YOoIOmDqOmVtw==\n" +
                        "# title:: ��?������ �����?\n" +
                        "givenname;lang-ja;phonetic:: 44KN44Gp44Gr44O8\n" +
                        "# givenname;lang-ja;phonetic:: �����������?\n" +
                        "sn;lang-ja;phonetic:: 44GK44GM44GV44KP44KJ\n" +
                        "# sn;lang-ja;phonetic:: ���������������\n" +
                        "cn;lang-ja;phonetic:: 44GK44GM44GV44KP44KJIOOCjeOBqeOBq+ODvA==\n" +
                        "# cn;lang-ja;phonetic:: ��������������� �����������?\n" +
                        "title;lang-ja;phonetic:: 44GI44GE44GO44KH44GG44G2IOOBtuOBoeOCh+OBhg==\n" +
                        "# title;lang-ja;phonetic::\n" +
                        "# �����������������? ��?���������\n" +
                        "givenname;lang-en: Rodney\n" +
                        "sn;lang-en: Ogasawara\n" +
                        "cn;lang-en: Rodney Ogasawara\n" +
                        "title;lang-en: Sales, Director\n";

        List entries = new LdifReader(ldif).asList();

        String[][][] values =
                {
                        {
                                {"dn", "ou=\u55b6\u696d\u90e8,o=Airius"}, // 55b6 = ��?, 696d = ���, 90e8 = ���
                                {"objectclass", "top"},
                                {"objectclass", "organizationalUnit"},
                                {"ou", "\u55b6\u696d\u90e8"},
                                {"ou;lang-ja", "\u55b6\u696d\u90e8"},
                                {"ou;lang-ja;phonetic", "\u3048\u3044\u304e\u3087\u3046\u3076"}, // 3048 = ���, 3044 = ���, 304e = ���
                                // 3087 = ���, 3046 = ���, 3076 = ��?
                                {"ou;lang-en", "Sales"},
                                {"description", "Japanese office"}},
                        {
                                {"dn", "uid=rogasawara,ou=\u55b6\u696d\u90e8,o=Airius"},
                                {"userpassword", "{SHA}O3HSv1MusyL4kTjP+HKI5uxuNoM="},
                                {"objectclass", "top"},
                                {"objectclass", "person"},
                                {"objectclass", "organizationalPerson"},
                                {"objectclass", "inetOrgPerson"},
                                {"uid", "rogasawara"},
                                {"mail", "rogasawara@airius.co.jp"},
                                {"givenname;lang-ja", "\u30ed\u30c9\u30cb\u30fc"},    // 30ed = ���, 30c9 = ���, 30cb = ���, 30fc = ��?
                                {"sn;lang-ja", "\u5c0f\u7b20\u539f"},    // 5c0f = �?�, 7b20 = ���, 539f = ���
                                {"cn;lang-ja", "\u5c0f\u7b20\u539f \u30ed\u30c9\u30cb\u30fc"},
                                {"title;lang-ja", "\u55b6\u696d\u90e8 \u90e8\u9577"},   // 9577 = ��?
                                {"preferredlanguage", "ja"},
                                {"givenname", "\u30ed\u30c9\u30cb\u30fc"},
                                {"sn", "\u5c0f\u7b20\u539f"},
                                {"cn", "\u5c0f\u7b20\u539f \u30ed\u30c9\u30cb\u30fc"},
                                {"title", "\u55b6\u696d\u90e8 \u90e8\u9577"},
                                {"givenname;lang-ja;phonetic", "\u308d\u3069\u306b\u30fc"},  // 308d = ���,3069 = ���, 306b = ���
                                {"sn;lang-ja;phonetic", "\u304a\u304c\u3055\u308f\u3089"}, // 304a = ���, 304c = ���,3055 = ���,308f = ���, 3089 = ���
                                {"cn;lang-ja;phonetic", "\u304a\u304c\u3055\u308f\u3089 \u308d\u3069\u306b\u30fc"},
                                {"title;lang-ja;phonetic", "\u3048\u3044\u304e\u3087\u3046\u3076 \u3076\u3061\u3087\u3046"}, // 304E = ���, 3061 = ���
                                {"givenname;lang-en", "Rodney"},
                                {"sn;lang-en", "Ogasawara"},
                                {"cn;lang-en", "Rodney Ogasawara"},
                                {"title;lang-en", "Sales, Director"}
                        }
                };

        assertEquals(2, entries.size());

        // Entry 1
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = (Entry) entries.get(i);
            assertTrue(entry.isChangeAdd());

            for (int j = 0; j < values[i].length; j++) {
                if ("dn".equalsIgnoreCase(values[i][j][0])) {
                    assertEquals(values[i][j][1], entry.getDn());
                } else {
                    Attribute attr = entry.get(values[i][j][0]);

                    if (attr.contains(values[i][j][1])) {
                        assertTrue(true);
                    } else {
                        assertTrue(attr.contains(values[i][j][1].getBytes("UTF-8")));
                    }
                }
            }
        }
    }

    public void testLdifParserRFC2849Sample5() throws NamingException, Exception {
        String ldif =
                "version: 1\n" +
                        "dn: cn=Horatio Jensen, ou=Product Testing, dc=airius, dc=com\n" +
                        "objectclass: top\n" +
                        "objectclass: person\n" +
                        "objectclass: organizationalPerson\n" +
                        "cn: Horatio Jensen\n" +
                        "cn: Horatio N Jensen\n" +
                        "sn: Jensen\n" +
                        "uid: hjensen\n" +
                        "telephonenumber: +1 408 555 1212\n" +
                        "jpegphoto:< file:" + HJENSEN_JPEG_FILE.getAbsolutePath() + "\n";

        List entries = new LdifReader(ldif).asList();

        String[][] values =
                {
                        {"dn", "cn=Horatio Jensen, ou=Product Testing, dc=airius, dc=com"},
                        {"objectclass", "top"},
                        {"objectclass", "person"},
                        {"objectclass", "organizationalPerson"},
                        {"cn", "Horatio Jensen"},
                        {"cn", "Horatio N Jensen"},
                        {"sn", "Jensen"},
                        {"uid", "hjensen"},
                        {"telephonenumber", "+1 408 555 1212"},
                        {"jpegphoto", null}
                };

        assertEquals(1, entries.size());

        // Entry 1
        Entry entry = (Entry) entries.get(0);
        assertTrue(entry.isChangeAdd());

        for (int i = 0; i < values.length; i++) {
            if ("dn".equalsIgnoreCase(values[i][0])) {
                assertEquals(values[i][1], entry.getDn());
            } else if ("jpegphoto".equalsIgnoreCase(values[i][0])) {
                Attribute attr = entry.get(values[i][0]);
                assertTrue(Arrays.equals(data, (byte[]) attr.get()));
            } else {
                Attribute attr = entry.get(values[i][0]);

                if (attr.contains(values[i][1])) {
                    assertTrue(true);
                } else {
                    assertTrue(attr.contains(values[i][1].getBytes("UTF-8")));
                }
            }
        }
    }

    public void testLdifParserRFC2849Sample5WithSizeLimit() throws NamingException, Exception {
        String ldif =
                "version: 1\n" +
                        "dn: cn=Horatio Jensen, ou=Product Testing, dc=airius, dc=com\n" +
                        "objectclass: top\n" +
                        "objectclass: person\n" +
                        "objectclass: organizationalPerson\n" +
                        "cn: Horatio Jensen\n" +
                        "cn: Horatio N Jensen\n" +
                        "sn: Jensen\n" +
                        "uid: hjensen\n" +
                        "telephonenumber: +1 408 555 1212\n" +
                        "jpegphoto:< file:" + HJENSEN_JPEG_FILE.getAbsolutePath() + "\n";

        try {
            new LdifReader(new LineIterator(new StringReader(ldif)), 128);
            fail();
        }
        catch (LdifParseException e) {
            final Throwable cause = e.getCause();
            assertTrue("Content too long exception must be thrown",
                    cause !=null && cause.getMessage().indexOf("too long")>0);
        }
    }

    public void testLdifParserRFC2849Sample6() throws NamingException, Exception {
        String ldif =
                "version: 1\n" +
                        // First entry modification : ADD
                        "# Add a new entry\n" +
                        "dn: cn=Fiona Jensen, ou=Marketing, dc=airius, dc=com\n" +
                        "changetype: add\n" +
                        "objectclass: top\n" +
                        "objectclass: person\n" +
                        "objectclass: organizationalPerson\n" +
                        "cn: Fiona Jensen\n" +
                        "sn: Jensen\n" +
                        "uid: fiona\n" +
                        "telephonenumber: +1 408 555 1212\n" +
                        "jpegphoto:< file:" + FIONA_JPEG_FILE.getAbsolutePath() + "\n" +
                        "\n" +
                        // Second entry modification : DELETE
                        "# Delete an existing entry\n" +
                        "dn: cn=Robert Jensen, ou=Marketing, dc=airius, dc=com\n" +
                        "changetype: delete\n" +
                        "\n" +
                        // Third entry modification : MODRDN
                        "# Modify an entry's relative distinguished name\n" +
                        "dn: cn=Paul Jensen, ou=Product Development, dc=airius, dc=com\n" +
                        "changetype: modrdn\n" +
                        "newrdn: cn=Paula Jensen\n" +
                        "deleteoldrdn: 1\n" +
                        "\n" +
                        // Forth entry modification : MODRDN
                        "# Rename an entry and move all of its children to a new location in\n" +
                        "# the directory tree (only implemented by LDAPv3 servers).\n" +
                        "dn: ou=PD Accountants, ou=Product Development, dc=airius, dc=com\n" +
                        "changetype: moddn\n" +
                        "newrdn: ou=Product Development Accountants\n" +
                        "deleteoldrdn: 0\n" +
                        "newsuperior: ou=Accounting, dc=airius, dc=com\n" +
                        "# Modify an entry: add an additional value to the postaladdress\n" +
                        "# attribute, completely delete the description attribute, replace\n" +
                        "# the telephonenumber attribute with two values, and delete a specific\n" +
                        "# value from the facsimiletelephonenumber attribute\n" +
                        "\n" +
                        // Fitfh entry modification : MODIFY
                        "dn: cn=Paula Jensen, ou=Product Development, dc=airius, dc=com\n" +
                        "changetype: modify\n" +
                        "add: postaladdress\n" +
                        "postaladdress: 123 Anystreet $ Sunnyvale, CA $ 94086\n" +
                        "-\n" +
                        "delete: description\n" +
                        "-\n" +
                        "replace: telephonenumber\n" +
                        "telephonenumber: +1 408 555 1234\n" +
                        "telephonenumber: +1 408 555 5678\n" +
                        "-\n" +
                        "delete: facsimiletelephonenumber\n" +
                        "facsimiletelephonenumber: +1 408 555 9876\n" +
                        "-\n" +
                        "\n" +
                        // Sixth entry modification : MODIFY
                        "# Modify an entry: replace the postaladdress attribute with an empty\n" +
                        "# set of values (which will cause the attribute to be removed), and\n" +
                        "# delete the entire description attribute. Note that the first will\n" +
                        "# always succeed, while the second will only succeed if at least\n" +
                        "# one value for the description attribute is present.\n" +
                        "dn: cn=Ingrid Jensen, ou=Product Support, dc=airius, dc=com\n" +
                        "changetype: modify\n" +
                        "replace: postaladdress\n" +
                        "-\n" +
                        "delete: description\n" +
                        "-\n";

        List entries = new LdifReader(ldif).asList();

        String[][][] values =
                {
                        // First entry modification : ADD
                        {
                                {"dn", "cn=Fiona Jensen, ou=Marketing, dc=airius, dc=com"},
                                {"objectclass", "top"},
                                {"objectclass", "person"},
                                {"objectclass", "organizationalPerson"},
                                {"cn", "Fiona Jensen"},
                                {"sn", "Jensen"},
                                {"uid", "fiona"},
                                {"telephonenumber", "+1 408 555 1212"},
                                {"jpegphoto", ""}
                        },
                        // Second entry modification : DELETE
                        {
                                {"dn", "cn=Robert Jensen, ou=Marketing, dc=airius, dc=com"}
                        },
                        // Third entry modification : MODRDN
                        {
                                {"dn", "cn=Paul Jensen, ou=Product Development, dc=airius, dc=com"},
                                {"cn=Paula Jensen"}
                        },
                        // Forth entry modification : MODRDN
                        {
                                {"dn", "ou=PD Accountants, ou=Product Development, dc=airius, dc=com"},
                                {"ou=Product Development Accountants"},
                                {"ou=Accounting, dc=airius, dc=com"}
                        },
                        // Fitfh entry modification : MODIFY
                        {
                                {"dn", "cn=Paula Jensen, ou=Product Development, dc=airius, dc=com"},
                                // add
                                {"postaladdress", "123 Anystreet $ Sunnyvale, CA $ 94086"},
                                // delete
                                {"description"},
                                // replace
                                {"telephonenumber", "+1 408 555 1234", "+1 408 555 5678"},
                                // delete
                                {"facsimiletelephonenumber", "+1 408 555 9876"},
                        },
                        // Sixth entry modification : MODIFY
                        {
                                {"dn", "cn=Ingrid Jensen, ou=Product Support, dc=airius, dc=com"},
                                // replace
                                {"postaladdress"},
                                // delete
                                {"description"}
                        }
                };

        Entry entry = (Entry) entries.get(0);
        assertTrue(entry.isChangeAdd());

        for (int i = 0; i < values.length; i++) {
            if ("dn".equalsIgnoreCase(values[0][i][0])) {
                assertEquals(values[0][i][1], entry.getDn());
            } else if ("jpegphoto".equalsIgnoreCase(values[0][i][0])) {
                Attribute attr = entry.get(values[0][i][0]);
                assertTrue(Arrays.equals(data, (byte[]) attr.get()));
            } else {
                Attribute attr = entry.get(values[0][i][0]);

                if (attr.contains(values[0][i][1])) {
                    assertTrue(true);
                } else {
                    assertTrue(attr.contains(values[0][i][1].getBytes("UTF-8")));
                }
            }
        }

        // Second entry
        entry = (Entry) entries.get(1);
        assertTrue(entry.isChangeDelete());
        assertEquals(values[1][0][1], entry.getDn());

        // Third entry
        entry = (Entry) entries.get(2);
        assertTrue(entry.isChangeModRdn());
        assertEquals(values[2][0][1], entry.getDn());
        assertEquals(values[2][1][0], entry.getNewRdn());
        assertTrue(entry.isDeleteOldRdn());

        // Forth entry
        entry = (Entry) entries.get(3);
        assertTrue(entry.isChangeModDn());
        assertEquals(values[3][0][1], entry.getDn());
        assertEquals(values[3][1][0], entry.getNewRdn());
        assertFalse(entry.isDeleteOldRdn());
        assertEquals(values[3][2][0], entry.getNewSuperior());

        // Fifth entry
        entry = (Entry) entries.get(4);
        List modifs = entry.getModificationItems();

        assertTrue(entry.isChangeModify());
        assertEquals(values[4][0][1], entry.getDn());

        // "add: postaladdress"
        // "postaladdress: 123 Anystreet $ Sunnyvale, CA $ 94086"
        ModificationItem item = (ModificationItem) modifs.get(0);
        assertEquals(DirContext.ADD_ATTRIBUTE, item.getModificationOp());
        assertEquals(values[4][1][0], item.getAttribute().getID());
        assertEquals(values[4][1][1], item.getAttribute().get(0));

        // "delete: description\n" +
        item = (ModificationItem) modifs.get(1);
        assertEquals(DirContext.REMOVE_ATTRIBUTE, item.getModificationOp());
        assertEquals(values[4][2][0], item.getAttribute().getID());

        // "replace: telephonenumber"
        // "telephonenumber: +1 408 555 1234"
        // "telephonenumber: +1 408 555 5678"
        item = (ModificationItem) modifs.get(2);
        assertEquals(DirContext.REPLACE_ATTRIBUTE, item.getModificationOp());
        assertEquals(values[4][3][0], item.getAttribute().getID());
        assertEquals(values[4][3][1], item.getAttribute().get(0));
        assertEquals(values[4][3][2], item.getAttribute().get(1));

        // "delete: facsimiletelephonenumber"
        // "facsimiletelephonenumber: +1 408 555 9876"
        item = (ModificationItem) modifs.get(3);
        assertEquals(DirContext.REMOVE_ATTRIBUTE, item.getModificationOp());
        assertEquals(values[4][4][0], item.getAttribute().getID());
        assertEquals(values[4][4][1], item.getAttribute().get(0));

        // Sixth entry
        entry = (Entry) entries.get(5);
        modifs = entry.getModificationItems();

        assertTrue(entry.isChangeModify());
        assertEquals(values[5][0][1], entry.getDn());

        // "replace: postaladdress"
        item = (ModificationItem) modifs.get(0);
        assertEquals(DirContext.REPLACE_ATTRIBUTE, item.getModificationOp());
        assertEquals(values[5][1][0], item.getAttribute().getID());

        // "delete: description"
        item = (ModificationItem) modifs.get(1);
        assertEquals(DirContext.REMOVE_ATTRIBUTE, item.getModificationOp());
        assertEquals(values[5][2][0], item.getAttribute().getID());
    }

    public void testLdifParserRFC2849Sample7() throws NamingException, Exception {
        String ldif =
                "version: 1\n" +
                        "# Delete an entry. The operation will attach the LDAPv3\n" +
                        "# Tree Delete Control defined in [9]. The criticality\n" +
                        "# field is \"true\" and the controlValue field is\n" +
                        "# absent, as required by [9].\n" +
                        "dn: ou=Product Development, dc=airius, dc=com\n" +
                        "control: 1.2.840.113556.1.4.805 true\n" +
                        "changetype: delete\n";

        List entries = new LdifReader(ldif).asList();

        Entry entry = (Entry) entries.get(0);

        assertEquals("ou=Product Development, dc=airius, dc=com", entry.getDn());
        assertTrue(entry.isChangeDelete());

        // Check the control
        Control control = entry.getControl();

        assertEquals("1.2.840.113556.1.4.805", control.getID());
        assertTrue(control.isCritical());
    }

    public void testLdifParserRFC2849Sample7NoValueNoCritical() throws NamingException, Exception {
        String ldif =
                "version: 1\n" +
                        "# Delete an entry. The operation will attach the LDAPv3\n" +
                        "# Tree Delete Control defined in [9]. The criticality\n" +
                        "# field is \"true\" and the controlValue field is\n" +
                        "# absent, as required by [9].\n" +
                        "dn: ou=Product Development, dc=airius, dc=com\n" +
                        "control: 1.2.840.11556.1.4.805\n" +
                        "changetype: delete\n";

        List entries = new LdifReader(ldif).asList();

        Entry entry = (Entry) entries.get(0);

        assertEquals("ou=Product Development, dc=airius, dc=com", entry.getDn());
        assertTrue(entry.isChangeDelete());

        // Check the control
        Control control = entry.getControl();

        assertEquals("1.2.840.11556.1.4.805", control.getID());
        assertFalse(control.isCritical());
    }

    public void testLdifParserRFC2849Sample7NoCritical() throws NamingException, Exception {
        String ldif =
                "version: 1\n" +
                        "# Delete an entry. The operation will attach the LDAPv3\n" +
                        "# Tree Delete Control defined in [9]. The criticality\n" +
                        "# field is \"true\" and the controlValue field is\n" +
                        "# absent, as required by [9].\n" +
                        "dn: ou=Product Development, dc=airius, dc=com\n" +
                        "control: 1.2.840.11556.1.4.805:control-value\n" +
                        "changetype: delete\n";

        List entries = new LdifReader(ldif).asList();

        Entry entry = (Entry) entries.get(0);

        assertEquals("ou=Product Development, dc=airius, dc=com", entry.getDn());
        assertTrue(entry.isChangeDelete());

        // Check the control
        Control control = entry.getControl();

        assertEquals("1.2.840.11556.1.4.805", control.getID());
        assertFalse(control.isCritical());
        assertEquals("control-value", new String(control.getEncodedValue()));
    }

    public void testLdifParserRFC2849Sample7NoOid() throws NamingException, Exception {
        String ldif =
                "version: 1\n" +
                        "# Delete an entry. The operation will attach the LDAPv3\n" +
                        "# Tree Delete Control defined in [9]. The criticality\n" +
                        "# field is \"true\" and the controlValue field is\n" +
                        "# absent, as required by [9].\n" +
                        "dn: ou=Product Development, dc=airius, dc=com\n" +
                        "control: true\n" +
                        "changetype: delete\n";

        try {
            new LdifReader(ldif).asList();
            fail();
        }
        catch (LdifParseException e) {
            //OK
        }
    }

    public void testLdifParserRFC2849Sample7BadOid() throws NamingException, Exception {
        String ldif =
                "version: 1\n" +
                        "# Delete an entry. The operation will attach the LDAPv3\n" +
                        "# Tree Delete Control defined in [9]. The criticality\n" +
                        "# field is \"true\" and the controlValue field is\n" +
                        "# absent, as required by [9].\n" +
                        "dn: ou=Product Development, dc=airius, dc=com\n" +
                        "control: 1.2.840.11A556.1.4.805 true\n" +
                        "changetype: delete\n";

        try {
            new LdifReader(ldif).asList();
            fail();
        }
        catch (LdifParseException e) {
            //OK
        }
    }

    public void testLdifParserChangeModifyMultiAttrs() throws NamingException, Exception {
        String ldif =
                "version: 1\n" +
                        "dn: ou=Product Development, dc=airius, dc=com\n" +
                        "changetype: modify\n" +
                        "add: postaladdress\n" +
                        "postaladdress: 123 Anystreet $ Sunnyvale, CA $ 94086\n" +
                        "-\n" +
                        "delete: postaladdress\n" +
                        "-\n" +
                        "replace: telephonenumber\n" +
                        "telephonenumber: +1 408 555 1234\n" +
                        "telephonenumber: +1 408 555 5678\n" +
                        "-\n" +
                        "delete: facsimiletelephonenumber\n" +
                        "facsimiletelephonenumber: +1 408 555 9876\n";

        try {
            new LdifReader(ldif).asList();
            fail();
        }
        catch (LdifParseException e) {
            //OK
        }
    }
}
