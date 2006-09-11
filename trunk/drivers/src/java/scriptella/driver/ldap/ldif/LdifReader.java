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

import scriptella.util.StringUtils;

import javax.naming.directory.DirContext;
import javax.naming.ldap.BasicControl;
import javax.naming.ldap.Control;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <pre>
 *  &lt;ldif-file&gt; ::= &quot;version:&quot; &lt;fill&gt; &lt;number&gt; &lt;seps&gt; &lt;dn-spec&gt; &lt;sep&gt; &lt;ldif-content-change&gt;
 * <p/>
 *  &lt;ldif-content-change&gt; ::=
 *    &lt;number&gt; &lt;oid&gt; &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; &lt;ldif-attrval-record-e&gt; |
 *    &lt;alpha&gt; &lt;chars-e&gt; &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; &lt;ldif-attrval-record-e&gt; |
 *    &quot;control:&quot; &lt;fill&gt; &lt;number&gt; &lt;oid&gt; &lt;spaces-e&gt; &lt;criticality&gt; &lt;value-spec-e&gt; &lt;sep&gt; &lt;controls-e&gt;
 *        &quot;changetype:&quot; &lt;fill&gt; &lt;changerecord-type&gt; &lt;ldif-change-record-e&gt; |
 *    &quot;changetype:&quot; &lt;fill&gt; &lt;changerecord-type&gt; &lt;ldif-change-record-e&gt;
 * <p/>
 *  &lt;ldif-attrval-record-e&gt; ::= &lt;seps&gt; &lt;dn-spec&gt; &lt;sep&gt; &lt;attributeType&gt;
 *    &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt;
 *    &lt;ldif-attrval-record-e&gt; | e
 * <p/>
 *  &lt;ldif-change-record-e&gt; ::= &lt;seps&gt; &lt;dn-spec&gt; &lt;sep&gt; &lt;controls-e&gt;
 *    &quot;changetype:&quot; &lt;fill&gt; &lt;changerecord-type&gt; &lt;ldif-change-record-e&gt; | e
 * <p/>
 *  &lt;dn-spec&gt; ::= &quot;dn:&quot; &lt;fill&gt; &lt;safe-string&gt; | &quot;dn::&quot; &lt;fill&gt; &lt;base64-string&gt;
 * <p/>
 *  &lt;controls-e&gt; ::= &quot;control:&quot; &lt;fill&gt; &lt;number&gt; &lt;oid&gt; &lt;spaces-e&gt; &lt;criticality&gt;
 *    &lt;value-spec-e&gt; &lt;sep&gt; &lt;controls-e&gt; | e
 * <p/>
 *  &lt;criticality&gt; ::= &quot;true&quot; | &quot;false&quot; | e
 * <p/>
 *  &lt;oid&gt; ::= '.' &lt;number&gt; &lt;oid&gt; | e
 * <p/>
 *  &lt;attrval-specs-e&gt; ::= &lt;number&gt; &lt;oid&gt; &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; |
 *    &lt;alpha&gt; &lt;chars-e&gt; &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; | e
 * <p/>
 *  &lt;value-spec-e&gt; ::= &lt;value-spec&gt; | e
 * <p/>
 *  &lt;value-spec&gt; ::= ':' &lt;fill&gt; &lt;safe-string-e&gt; |
 *    &quot;::&quot; &lt;fill&gt; &lt;base64-chars&gt; |
 *    &quot;:&lt;&quot; &lt;fill&gt; &lt;url&gt;
 * <p/>
 *  &lt;attributeType&gt; ::= &lt;number&gt; &lt;oid&gt; | &lt;alpha&gt; &lt;chars-e&gt;
 * <p/>
 *  &lt;options-e&gt; ::= ';' &lt;char&gt; &lt;chars-e&gt; &lt;options-e&gt; |e
 * <p/>
 *  &lt;chars-e&gt; ::= &lt;char&gt; &lt;chars-e&gt; |  e
 * <p/>
 *  &lt;changerecord-type&gt; ::= &quot;add&quot; &lt;sep&gt; &lt;attributeType&gt; &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; |
 *    &quot;delete&quot; &lt;sep&gt; |
 *    &quot;modify&quot; &lt;sep&gt; &lt;mod-type&gt; &lt;fill&gt; &lt;attributeType&gt; &lt;options-e&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; &lt;sep&gt; '-' &lt;sep&gt; &lt;mod-specs-e&gt; |
 *    &quot;moddn&quot; &lt;sep&gt; &lt;newrdn&gt; &lt;sep&gt; &quot;deleteoldrdn:&quot; &lt;fill&gt; &lt;0-1&gt; &lt;sep&gt; &lt;newsuperior-e&gt; &lt;sep&gt; |
 *    &quot;modrdn&quot; &lt;sep&gt; &lt;newrdn&gt; &lt;sep&gt; &quot;deleteoldrdn:&quot; &lt;fill&gt; &lt;0-1&gt; &lt;sep&gt; &lt;newsuperior-e&gt; &lt;sep&gt;
 * <p/>
 *  &lt;newrdn&gt; ::= ':' &lt;fill&gt; &lt;safe-string&gt; | &quot;::&quot; &lt;fill&gt; &lt;base64-chars&gt;
 * <p/>
 *  &lt;newsuperior-e&gt; ::= &quot;newsuperior&quot; &lt;newrdn&gt; | e
 * <p/>
 *  &lt;mod-specs-e&gt; ::= &lt;mod-type&gt; &lt;fill&gt; &lt;attributeType&gt; &lt;options-e&gt;
 *    &lt;sep&gt; &lt;attrval-specs-e&gt; &lt;sep&gt; '-' &lt;sep&gt; &lt;mod-specs-e&gt; | e
 * <p/>
 *  &lt;mod-type&gt; ::= &quot;add:&quot; | &quot;delete:&quot; | &quot;replace:&quot;
 * <p/>
 *  &lt;url&gt; ::= &lt;a Uniform Resource Locator, as defined in [6]&gt;
 * <p/>
 * <p/>
 * <p/>
 *  LEXICAL
 *  -------
 * <p/>
 *  &lt;fill&gt;           ::= ' ' &lt;fill&gt; | e
 *  &lt;char&gt;           ::= &lt;alpha&gt; | &lt;digit&gt; | '-'
 *  &lt;number&gt;         ::= &lt;digit&gt; &lt;digits&gt;
 *  &lt;0-1&gt;            ::= '0' | '1'
 *  &lt;digits&gt;         ::= &lt;digit&gt; &lt;digits&gt; | e
 *  &lt;digit&gt;          ::= '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
 *  &lt;seps&gt;           ::= &lt;sep&gt; &lt;seps-e&gt;
 *  &lt;seps-e&gt;         ::= &lt;sep&gt; &lt;seps-e&gt; | e
 *  &lt;sep&gt;            ::= 0x0D 0x0A | 0x0A
 *  &lt;spaces&gt;         ::= ' ' &lt;spaces-e&gt;
 *  &lt;spaces-e&gt;       ::= ' ' &lt;spaces-e&gt; | e
 *  &lt;safe-string-e&gt;  ::= &lt;safe-string&gt; | e
 *  &lt;safe-string&gt;    ::= &lt;safe-init-char&gt; &lt;safe-chars&gt;
 *  &lt;safe-init-char&gt; ::= [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x1F] | [0x21-0x39] | 0x3B | [0x3D-0x7F]
 *  &lt;safe-chars&gt;     ::= &lt;safe-char&gt; &lt;safe-chars&gt; | e
 *  &lt;safe-char&gt;      ::= [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x7F]
 *  &lt;base64-string&gt;  ::= &lt;base64-char&gt; &lt;base64-chars&gt;
 *  &lt;base64-chars&gt;   ::= &lt;base64-char&gt; &lt;base64-chars&gt; | e
 *  &lt;base64-char&gt;    ::= 0x2B | 0x2F | [0x30-0x39] | 0x3D | [0x41-9x5A] | [0x61-0x7A]
 *  &lt;alpha&gt;          ::= [0x41-0x5A] | [0x61-0x7A]
 * <p/>
 *  COMMENTS
 *  --------
 *  - The ldap-oid VN is not correct in the RFC-2849. It has been changed from 1*DIGIT 0*1(&quot;.&quot; 1*DIGIT) to
 *  DIGIT+ (&quot;.&quot; DIGIT+)*
 *  - The mod-spec lacks a sep between *attrval-spec and &quot;-&quot;.
 *  - The BASE64-UTF8-STRING should be BASE64-CHAR BASE64-STRING
 *  - The ValueSpec rule must accept multilines values. In this case, we have a LF followed by a
 *  single space before the continued value.
 * </pre>
 */
public class LdifReader implements Iterator<Entry>, Iterable<Entry> {

    /**
     * A list of read lines
     */
    private final List<String> lines=new ArrayList<String>();

    /**
     * The ldif file version default value
     */
    private static final int DEFAULT_VERSION = 1;

    /**
     * The ldif version
     */
    private int version=DEFAULT_VERSION;

    /**
     * Type of element read
     */
    private static final int ENTRY = 0;

    private static final int CHANGE = 1;

    private static final int UNKNOWN = 2;

    /**
     * Size limit for file contained values
     */
    private long sizeLimit = SIZE_LIMIT_DEFAULT;

    /**
     * The default size limit : 1Mo
     */
    private static final long SIZE_LIMIT_DEFAULT = 1024000;

    /**
     * State values for the modify operation
     */
    private static final int MOD_SPEC = 0;

    private static final int ATTRVAL_SPEC = 1;

    private static final int ATTRVAL_SPEC_OR_SEP = 2;

    /**
     * Iterator prefetched entry
     */
    private Entry prefetched;

    /**
     * The ldif Reader
     */
    private Reader in;

    /**
     * A flag set if the ldif contains entries
     */
    private boolean containsEntries;

    /**
     * A flag set if the ldif contains changes
     */
    private boolean containsChanges;


    /**
     * Constructors
     */
    public LdifReader() {
    }

    private void init(BufferedReader in) throws LdifParseException {
        this.in = in;

        // First get the version - if any -
        version = parseVersion();
        prefetched = parseEntry();
    }

    /**
     * A constructor which takes a Reader
     *
     * @param in A Reader containing ldif formated input
     * @throws LdifParseException If the file cannot be processed or if the format is incorrect
     */
    public LdifReader(Reader in) {
        if (in instanceof BufferedReader) {//check to avoid double buffering
            init((BufferedReader) in);
        } else {
            init(new BufferedReader(in));
        }
    }

    /**
     * @return The ldif file version
     */
    public int getVersion() {
        return version;
    }

    /**
     * @return The maximum size of a file which is used into an attribute value.
     */
    public long getSizeLimit() {
        return sizeLimit;
    }

    /**
     * Set the maximum file size that can be accepted for an attribute value
     *
     * @param sizeLimit The size in bytes
     */
    public void setSizeLimit(long sizeLimit) {
        this.sizeLimit = sizeLimit;
    }


    /**
     * Parse the changeType
     *
     * @param line The line which contains the changeType
     * @return The operation.
     */
    private int parseChangeType(String line) {
        int operation = Entry.ADD;

        String modOp = line.substring("changetype:".length() + 1).trim();

        if ("add".equalsIgnoreCase(modOp)) {
            operation = Entry.ADD;
        } else if ("delete".equalsIgnoreCase(modOp)) {
            operation = Entry.DELETE;
        } else if ("modify".equalsIgnoreCase(modOp)) {
            operation = Entry.MODIFY;
        } else if ("moddn".equalsIgnoreCase(modOp)) {
            operation = Entry.MODDN;
        } else if ("modrdn".equalsIgnoreCase(modOp)) {
            operation = Entry.MODRDN;
        }

        return operation;
    }

    /**
     * Parse the DN of an entry
     *
     * @param line The line to parse
     * @return A DN
     */
    private String parseDn(String line) {
        String dn = null;

        String lowerLine = line.toLowerCase();

        if (lowerLine.startsWith("dn:") || lowerLine.startsWith("DN:")) {
            // Ok, we have a DN. Is it base 64 encoded ?
            int length = line.length();

            if (length == 3) {
                // The DN is empty : error
                throw new LdifParseException("No DN for entry", line);
            } else if (line.charAt(3) == ':') {
                if (length > 4) {
                    // This is a base 64 encoded DN.
                    String trimmedLine = line.substring(4).trim();

                    try {
                        dn = new String(Utils.base64Decode(trimmedLine.toCharArray()), "UTF-8");
                    }
                    catch (UnsupportedEncodingException uee) {
                        // The DN is not base 64 encoded
                        throw new LdifParseException("Invalid base 64 encoded DN",line);
                    }
                } else {
                    // The DN is empty : error
                    throw new LdifParseException("No DN for entry", line);
                }
            } else {
                dn = line.substring(3).trim();
            }
        } else {
            throw new LdifParseException("No DN for entry", line);
        }

        return dn;
    }

    /**
     * Parse the value part.
     *
     * @param line The line which contains the value
     * @param pos  The starting position in the line
     * @return A String or a byte[], depending of the kind of value we get
     * @throws LdifParseException If something went wrong
     */
    private Object parseValue(String line, int pos) {
        if (line.length() > pos + 1) {
            char c = line.charAt(pos + 1);

            if (c == ':') {
                String value = line.substring(pos + 2).trim();

                return Utils.base64Decode(value.toCharArray());
            } else if (c == '<') {
                String urlName = line.substring(pos + 2).trim();
                try {
                    return Utils.toByteArray(getUriStream(urlName), sizeLimit);
                } catch (IOException e) {
                    throw new LdifParseException("Failed to read \""+urlName+"\" file content",line,e);
                }
            } else {
                return line.substring(pos + 1).trim();
            }
        } else {
            return null;
        }
    }

    /**
     * Resolves URI to URL and returns a content stream.
     * This method just creates a new URL, subclasses may chnange this behaviour.
     * @param uri URI to resolve.
     * @return resolved URL content stream.
     * @throws IOException if an I/O error occurs or URI is malformed.
     */
    protected InputStream getUriStream(String uri) throws IOException {
        return new URL(uri).openStream();
    }

    /**
     * Parse a control. The grammar is : <control> ::= "control:" <fill>
     * <ldap-oid> <critical-e> <value-spec-e> <sep> <critical-e> ::= <spaces>
     * <boolean> | e <boolean> ::= "true" | "false" <value-spec-e> ::=
     * <value-spec> | e <value-spec> ::= ":" <fill> <SAFE-STRING-e> | "::"
     * <fill> <BASE64-STRING> | ":<" <fill> <url>
     * <p/>
     * It can be read as : "control:" <fill> <ldap-oid> [ " "+ ( "true" |
     * "false") ] [ ":" <fill> <SAFE-STRING-e> | "::" <fill> <BASE64-STRING> | ":<"
     * <fill> <url> ]
     *
     * @param line The line containing the control
     * @return A control
     */
    private Control parseControl(String line) {
        String lowerLine = line.toLowerCase().trim();
        char[] controlValue = line.trim().toCharArray();
        int pos = 0;
        int length = controlValue.length;

        // Get the <ldap-oid>
        if (pos > length) {
            // No OID : error !
            throw new LdifParseException("Bad control, no oid", line);
        }

        int initPos = pos;

        while (Utils.isCharASCII(controlValue, pos, '.') || Utils.isDigit(controlValue, pos)) {
            pos++;
        }

        if (pos == initPos) {
            // Not a valid OID !
            throw new LdifParseException("Bad control, no oid", line);
        }


        String oid = lowerLine.substring(0, pos);
        boolean criticality=false;
        byte[] controlBytes = null;



        // Get the criticality, if any
        // Skip the <fill>
        while (Utils.isCharASCII(controlValue, pos, ' ')) {
            pos++;
        }

        // Check if we have a "true" or a "false"
        int criticalPos = lowerLine.indexOf(':');

        int criticalLength = 0;

        if (criticalPos == -1) {
            criticalLength = length - pos;
        } else {
            criticalLength = criticalPos - pos;
        }

        if ((criticalLength == 4) && ("true".equalsIgnoreCase(lowerLine.substring(pos, pos + 4)))) {
            criticality=true;
        } else if ((criticalLength == 5) && ("false".equalsIgnoreCase(lowerLine.substring(pos, pos + 5)))) {
            criticality=false;
        } else if (criticalLength != 0) {
            // If we have a criticality, it should be either "true" or "false",
            // nothing else
            throw new LdifParseException("Bad control criticality", line);
        }

        if (criticalPos > 0) {
            // We have a value. It can be a normal value, a base64 encoded value
            // or a file contained value
            if (Utils.isCharASCII(controlValue, criticalPos + 1, ':')) {
                // Base 64 encoded value
                controlBytes = Utils.base64Decode(line.substring(criticalPos + 2).toCharArray());
            } else if (Utils.isCharASCII(controlValue, criticalPos + 1, '<')) {
                // File contained value
            } else {
                // Standard value
                byte[] value = new byte[length - criticalPos - 1];

                for (int i = 0; i < length - criticalPos - 1; i++) {
                    value[i] = (byte) controlValue[i + criticalPos + 1];
                }

                controlBytes=value;
            }
        }

        return new BasicControl(oid, criticality, controlBytes);
    }


    /**
     * Parse an AttributeType/AttributeValue
     *
     * @param entry     The entry where to store the value
     * @param line      The line to parse
     */
    public void parseAttributeValue(Entry entry, String line) {
        int colonIndex = line.indexOf(':');

        String attributeType = line.substring(0, colonIndex);

        // We should *not* have a DN twice
        if (attributeType.equalsIgnoreCase("dn")) {
            throw new LdifParseException("A ldif entry should not have two DN", line);
        }

        Object attributeValue = parseValue(line, colonIndex);

        // Update the entry
        entry.addAttribute(attributeType, attributeValue);
    }

    /**
     * Parse a ModRDN operation
     *
     * @param entry The entry to update
     * @param iter  The lines iterator
     */
    private void parseModRdn(Entry entry, Iterator iter) {
        // We must have two lines : one starting with "newrdn:" or "newrdn::",
        // and the second starting with "deleteoldrdn:"
        if (iter.hasNext()) {
            String line = (String) iter.next();
            String lowerLine = line.toLowerCase();

            if (lowerLine.startsWith("newrdn::") || lowerLine.startsWith("newrdn:")) {
                int colonIndex = line.indexOf(':');
                Object attributeValue = parseValue(line, colonIndex);
                entry.setNewRdn(attributeValue instanceof String ? (String) attributeValue : Utils
                        .utf8ToString((byte[]) attributeValue));
            } else {
                throw new LdifParseException("Bad modrdn operation", line);
            }

        } else {
            throw new LdifParseException("Bad modrdn operation, no newrdn");
        }

        if (iter.hasNext()) {
            String line = (String) iter.next();
            String lowerLine = line.toLowerCase();

            if (lowerLine.startsWith("deleteoldrdn:")) {
                int colonIndex = line.indexOf(':');
                Object attributeValue = parseValue(line, colonIndex);
                entry.setDeleteOldRdn("1".equals(attributeValue));
            } else {
                throw new LdifParseException("Bad modrdn operation, no deleteoldrdn", line);
            }
        } else {
            throw new LdifParseException("Bad modrdn operation, no deleteoldrdn");
        }

    }

    /**
     * Parse a modify change type.
     * <p/>
     * The grammar is : <changerecord> ::= "changetype:" FILL "modify" SEP
     * <mod-spec> <mod-specs-e> <mod-spec> ::= "add:" <mod-val> | "delete:"
     * <mod-val-del> | "replace:" <mod-val> <mod-specs-e> ::= <mod-spec>
     * <mod-specs-e> | e <mod-val> ::= FILL ATTRIBUTE-DESCRIPTION SEP
     * ATTRVAL-SPEC <attrval-specs-e> "-" SEP <mod-val-del> ::= FILL
     * ATTRIBUTE-DESCRIPTION SEP <attrval-specs-e> "-" SEP <attrval-specs-e> ::=
     * ATTRVAL-SPEC <attrval-specs> | e *
     *
     * @param entry The entry to feed
     * @param iter  The lines
     */
    private void parseModify(Entry entry, Iterator iter) {
        int state = MOD_SPEC;
        String modified = null;
        int modification = 0;

        // The following flag is used to deal with empty modifications
        boolean isEmptyValue = true;

        while (iter.hasNext()) {
            String line = (String) iter.next();
            String lowerLine = line.toLowerCase();

            if (lowerLine.startsWith("-")) {
                if (state != ATTRVAL_SPEC_OR_SEP) {
                    throw new LdifParseException("Bad modify separator", line);
                } else {
                    if (isEmptyValue) {
                        // Update the entry
                        entry.addModificationItem(modification, modified, null);
                    }

                    state = MOD_SPEC;
                    isEmptyValue = true;
                    continue;
                }
            } else if (lowerLine.startsWith("add:")) {
                if ((state != MOD_SPEC) && (state != ATTRVAL_SPEC)) {
                    throw new LdifParseException("Bad modify state", line);
                }

                modified = line.substring("add:".length()).trim();
                modification = DirContext.ADD_ATTRIBUTE;

                state = ATTRVAL_SPEC;
            } else if (lowerLine.startsWith("delete:")) {
                if ((state != MOD_SPEC) && (state != ATTRVAL_SPEC)) {
                    throw new LdifParseException("Bad modify state", line);
                }

                modified = line.substring("delete:".length()).trim();
                modification = DirContext.REMOVE_ATTRIBUTE;

                state = ATTRVAL_SPEC_OR_SEP;
            } else if (lowerLine.startsWith("replace:")) {
                if ((state != MOD_SPEC) && (state != ATTRVAL_SPEC)) {
                    throw new LdifParseException("Bad modify state", line);
                }

                modified = line.substring("replace:".length()).trim();
                modification = DirContext.REPLACE_ATTRIBUTE;

                state = ATTRVAL_SPEC_OR_SEP;
            } else {
                if ((state != ATTRVAL_SPEC) && (state != ATTRVAL_SPEC_OR_SEP)) {
                    throw new LdifParseException("Bad modify state", line);
                }

                // A standard AttributeType/AttributeValue pair
                int colonIndex = line.indexOf(':');

                String attributeType = line.substring(0, colonIndex);

                if (!attributeType.equals(modified)) {
                    throw new LdifParseException("Bad modify attribute", line);
                }

                // We should *not* have a DN twice
                if (attributeType.equals("dn")) {
                    throw new LdifParseException("A ldif entry should not have two DN", line);
                }

                Object attributeValue = parseValue(line, colonIndex);

                // Update the entry
                entry.addModificationItem(modification, attributeType, attributeValue);
                isEmptyValue = false;

                state = ATTRVAL_SPEC_OR_SEP;
            }
        }
    }

    /**
     * Parse a change operation. We have to handle different cases depending on
     * the operation. 1) Delete : there should *not* be any line after the
     * "changetype: delete" 2) Add : we must have a list of AttributeType :
     * AttributeValue elements 3) ModDN : we must have two following lines: a
     * "newrdn:" and a "deleteoldrdn:" 4) ModRDN : the very same, but a
     * "newsuperior:" line is expected 5) Modify :
     * <p/>
     * The grammar is : <changerecord> ::= "changetype:" FILL "add" SEP
     * <attrval-spec> <attrval-specs-e> | "changetype:" FILL "delete" |
     * "changetype:" FILL "modrdn" SEP <newrdn> SEP <deleteoldrdn> SEP | // To
     * be checked "changetype:" FILL "moddn" SEP <newrdn> SEP <deleteoldrdn> SEP
     * <newsuperior> SEP | "changetype:" FILL "modify" SEP <mod-spec>
     * <mod-specs-e> <newrdn> ::= "newrdn:" FILL RDN | "newrdn::" FILL
     * BASE64-RDN <deleteoldrdn> ::= "deleteoldrdn:" FILL "0" | "deleteoldrdn:"
     * FILL "1" <newsuperior> ::= "newsuperior:" FILL DN | "newsuperior::" FILL
     * BASE64-DN <mod-specs-e> ::= <mod-spec> <mod-specs-e> | e <mod-spec> ::=
     * "add:" <mod-val> | "delete:" <mod-val> | "replace:" <mod-val> <mod-val>
     * ::= FILL ATTRIBUTE-DESCRIPTION SEP ATTRVAL-SPEC <attrval-specs-e> "-" SEP
     * <attrval-specs-e> ::= ATTRVAL-SPEC <attrval-specs> | e
     *
     * @param entry     The entry to feed
     * @param iter      The lines iterator
     * @param operation The change operation (add, modify, delete, moddn or modrdn)
     * @param control   The associated control, if any
     */
    private void parseChange(Entry entry, Iterator iter, int operation, Control control) {
        // The changetype and operation has already been parsed.
        entry.setChangeType(operation);

        switch (operation) {
            case Entry.DELETE:
                // The change type will tell that it's a delete operation,
                // the dn is used as a key.
                return;

            case Entry.ADD:
                // We will iterate through all attribute/value pairs
                while (iter.hasNext()) {
                    String line = (String) iter.next();
                    parseAttributeValue(entry, line);
                }

                return;

            case Entry.MODIFY:
                parseModify(entry, iter);
                return;

            case Entry.MODRDN:// They are supposed to have the same syntax ???
            case Entry.MODDN:
                // First, parse the modrdn part
                parseModRdn(entry, iter);

                // The next line should be the new superior
                if (iter.hasNext()) {
                    String line = (String) iter.next();
                    String lowerLine = line.toLowerCase();

                    if (lowerLine.startsWith("newsuperior:")) {
                        int colonIndex = line.indexOf(':');
                        Object attributeValue = parseValue(line, colonIndex);
                        entry.setNewSuperior(attributeValue instanceof String ? (String) attributeValue : Utils
                                .utf8ToString((byte[]) attributeValue));
                    } else {
                        if (operation == Entry.MODDN) {
                            throw new LdifParseException("Bad moddn operation, no newsuperior", line);
                        }
                    }
                } else {
                    if (operation == Entry.MODDN) {
                        throw new LdifParseException("Bad moddn operation, no newsuperior");
                    }
                }

                return;

            default:
                // This is an error
                throw new LdifParseException("Bad operation");
        }
    }

    /**
     * Parse a ldif file. The following rules are processed :
     * <p/>
     * <ldif-file> ::= <ldif-attrval-record> <ldif-attrval-records> |
     * <ldif-change-record> <ldif-change-records> <ldif-attrval-record> ::=
     * <dn-spec> <sep> <attrval-spec> <attrval-specs> <ldif-change-record> ::=
     * <dn-spec> <sep> <controls-e> <changerecord> <dn-spec> ::= "dn:" <fill>
     * <distinguishedName> | "dn::" <fill> <base64-distinguishedName>
     * <changerecord> ::= "changetype:" <fill> <change-op>
     */
    private Entry parseEntry() {
        if ((lines == null) || (lines.size() == 0)) {
            return null;
        }

        // The entry must start with a dn: or a dn::
        String line = lines.get(0);

        String dn = parseDn(line);

        // Ok, we have found a DN
        Entry entry = new Entry();
        entry.setDn(dn);

        // We remove this dn from the lines
        lines.remove(0);

        // Now, let's iterate through the other lines
        Iterator iter = lines.iterator();

        // This flag is used to distinguish between an entry and a change
        int type = UNKNOWN;

        // The following boolean is used to check that a control is *not*
        // found elswhere than just after the dn
        boolean controlSeen = false;

        // We use this boolean to check that we do not have AttributeValues
        // after a change operation
        boolean changeTypeSeen = false;

        int operation = Entry.ADD;
        String lowerLine = null;
        Control control = null;

        while (iter.hasNext()) {
            // Each line could start either with an OID, an attribute type, with
            // "control:" or with "changetype:"
            line = (String) iter.next();
            lowerLine = line.toLowerCase();

            // We have three cases :
            // 1) The first line after the DN is a "control:"
            // 2) The first line after the DN is a "changeType:"
            // 3) The first line after the DN is anything else
            if (lowerLine.startsWith("control:")) {
                if (containsEntries) {
                    throw new LdifParseException("No changes withing entries", line);
                }

                containsChanges = true;

                if (controlSeen) {
                    throw new LdifParseException("Control misplaced", line);
                }

                // Parse the control
                control = parseControl(line.substring("control:".length()));
                entry.setControl(control);

            } else if (lowerLine.startsWith("changetype:")) {
                if (containsEntries) {
                    throw new LdifParseException("No changes withing entries", line);
                }

                containsChanges = true;

                if (changeTypeSeen) {
                    throw new LdifParseException("ChangeType misplaced", line);
                }

                // A change request
                type = CHANGE;
                controlSeen = true;

                operation = parseChangeType(line);

                // Parse the change operation in a separate function
                parseChange(entry, iter, operation, control);
                changeTypeSeen = true;
            } else if (line.indexOf(':') > 0) {
                if (containsChanges) {
                    throw new LdifParseException("No entries within changes", line);
                }

                containsEntries = true;

                if (controlSeen || changeTypeSeen) {
                    throw new LdifParseException("AttributeType misplaced", line);
                }

                parseAttributeValue(entry, line);
                type = ENTRY;
            } else {
                // Invalid attribute Value
                throw new LdifParseException("Bad attribute", line);
            }
        }

        if (type == CHANGE) {
            entry.setChangeType(operation);
        }

        return entry;
    }

    /**
     * "version:" <fill> <number>
     */
    static final Pattern VERSION_PATTERN = Pattern.compile("[ ]*version\\:[ ]*(\\d+)[ ]*");

    /**
     * "version:" <fill>
     *  <number>
     */

    static final Pattern VERSION_PATTERN_LINE1 = Pattern.compile("[ ]*version\\:[ ]*");
    static final Pattern VERSION_PATTERN_LINE2 = Pattern.compile("[ ]\\d+");

    /**
     * Parse the version from the ldif input.
     *
     * @return A number representing the version (default to 1)
     */
    private int parseVersion() {

        // First, read a list of lines
        readLines();

        if (lines.size() == 0) {
            return DEFAULT_VERSION;
        }

        // get the first line
        String line = lines.get(0);


        Matcher versionMatcher = VERSION_PATTERN.matcher(line);
        String versionStr = null;

        if (versionMatcher.matches()) {
            versionStr=versionMatcher.group(1);
            // We have found the version, just discard the line from the list
            lines.remove(0);
        } else {
            versionMatcher = VERSION_PATTERN_LINE1.matcher(line);
            if (versionMatcher.matches()) {
                lines.remove(0);
                if (!lines.isEmpty()) {
                    versionMatcher = VERSION_PATTERN_LINE2.matcher(lines.get(1));
                    if (versionMatcher.matches()) {
                        versionStr=versionMatcher.group(1);
                    }
                    lines.remove(0);
                }

            }

        }

        if (versionStr!=null) {
            try {
                return Integer.parseInt(versionStr.trim());
            } catch (NumberFormatException e) {
                throw new LdifParseException("Invalid LDIF version number "+versionStr, line);
            }
        } else {
            return DEFAULT_VERSION;
        }
    }

    /**
     * Reads an entry in a ldif buffer, and returns the resulting lines, without
     * comments, and unfolded.
     * <p/>
     * The lines represent *one* entry.
     *
     */
    private void readLines() {
        String line;
        boolean insideComment = true;
        boolean isFirstLine = true;

        lines.clear();
        StringBuilder sb = new StringBuilder(128);

        try {
            while ((line = ((BufferedReader) in).readLine()) != null) { //while not EOF
                if (StringUtils.isAsciiWhitespacesOnly(line)) { //if line is empty
                    if (isFirstLine) {
                        continue;
                    } else {
                        // The line is empty, we have read an entry
                        insideComment = false;
                        if (lines.isEmpty()) { //if block is empty, i.e. comments section - read the next entry
                            continue;
                        } else { //otherwise stop
                            break;
                        }
                    }
                }

                isFirstLine = false;

                // We will read the first line which is not a comment
                switch (line.charAt(0)) {
                    case '#':
                        insideComment = true;
                        break;

                    case ' ':
                        if (insideComment) {
                            continue;
                        } else if (sb.length() == 0) {
                            throw new LdifParseException("Ldif Parsing error: Cannot have an empty continuation line");
                        } else {
                            sb.append(line.substring(1));
                        }

                        insideComment = false;
                        break;

                    default:
                        // We have found a new entry
                        // First, stores the previous one if any.
                        if (sb.length() != 0) {
                            lines.add(sb.toString());
                        }

                        sb = new StringBuilder(line);
                        insideComment = false;
                        break;
                }
            }
        }
        catch (IOException ioe) {
            throw new LdifParseException("Error while reading ldif lines");
        }

        // Stores the current line if necessary.
        if (sb.length() != 0) {
            lines.add(sb.toString());
        }

    }


    // ------------------------------------------------------------------------
    // Iterator Methods
    // ------------------------------------------------------------------------

    /**
     * Gets the next LDIF on the channel.
     *
     * @return the next LDIF as a String.
     */
    public Entry next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No LDIF entries to read. Use hasNext().");
        }
        Entry res = prefetched;
        prefetched=null;
        return res;
    }

    /**
     * Tests to see if another LDIF is on the input channel.
     *
     * @return true if another LDIF is available false otherwise.
     */
    public boolean hasNext() {
        if (prefetched==null) {
            readLines();
            prefetched = parseEntry();
        }
        return null != prefetched;
    }

    /**
     * Always throws UnsupportedOperationException!
     *
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return An iterator on the file
     */
    public Iterator<Entry> iterator() {
        return this;
    }


    List parseLdif(String s) {
        return parseLdif(new BufferedReader(new StringReader(s)));
    }

    /**
     * The main entry point of the LdifParser. It reads a buffer and returns a
     * List of entries.
     *
     * @param in The buffer being processed
     * @return A list of entries
     * @throws LdifParseException If something went wrong
     */
    public List<Entry> parseLdif(BufferedReader in) {
        init(in);
        // Create a list that will contain the read entries
        List<Entry> entries = new ArrayList<Entry>();

        // When done, get the entries one by one.
        while (hasNext()) {
            Entry entry = next();
            entries.add(entry);
        }

        return entries;
    }

    /**
     * @return True if the ldif file contains entries, fals if it contains
     *         changes
     */
    public boolean containsEntries() {
        return containsEntries;
    }





}