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
package scriptella.sql;

import scriptella.configuration.ConfigurationException;
import scriptella.expressions.PropertiesSubstitutor;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Customizable SQL parser.
 * <p><u>Supported extensions</u>
 * The parser supports extensions described in {@link PropertiesSubstitutor}.
 * <i>Notes:</i><br>
 * SQL quoted expressions are not substituted. Example:
 * <pre><code>
 * SELECT * FROM "Table" WHERE NAME="John${prop}" and SURNAME=?surname; --only SURNAME is handled
 * </code></pre>
 * These extensions are handled by subclasses in {@link #handleParameter(String, boolean)} method.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SQLParserBase {
    private static final Pattern EMPTY_PTR = Pattern.compile("\\s*");

    /**
     * Parses SQL script.
     *
     * @param reader reader with SQL script.
     */
    public void parse(final Reader reader) {
        StreamTokenizer st = new StreamTokenizer(reader);

        try {
            parse(st);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
    }

    private void parse(final StreamTokenizer st) throws IOException {
        st.resetSyntax();
        st.wordChars(' ', 255);
        st.commentChar('/');
        st.commentChar('-');
        st.ordinaryChar(';');
        st.ordinaryChar('$');
        st.quoteChar('"');
        st.quoteChar('\'');

        StringBuilder b = new StringBuilder(80);
        List<Integer> injections = null;

        for (int token; (token = st.nextToken()) != StreamTokenizer.TT_EOF;) {
            switch (token) {
                case StreamTokenizer.TT_WORD:
                    // A word was found; the value is in sval
                    //                    System.out.println("Word " + st.sval + " line=" + line);
                    b.append(st.sval);

                    break;

                case '"':
                    // A double-quoted string was found; sval contains the contents
                    //                    System.out.println("Quote \"" + st.sval + "\"");
                    b.append('"');
                    b.append(st.sval);
                    b.append('"');

                    break;

                case '\'':
                    // A single-quoted string was found; sval contains the contents
                    //                    System.out.println("Quote '" + st.sval + "'");
                    b.append('\'');
                    b.append(st.sval);
                    b.append('\'');

                    break;

                case StreamTokenizer.TT_EOL:
                    // End of line character found
                    //                    System.out.println("EOL");
                    b.append(' ');

                    break;

                default:

                    // A regular character was found; the value is the token itself
                    char ch = (char) st.ttype;

                    switch (ch) {
                        case ';':
                            handleStatement(b, injections);
                            b.setLength(0);
                            if (injections!=null) {
                                injections.clear();
                            }
                            break;
                        case '$':
                            if (injections==null) {
                                injections=new ArrayList<Integer>();
                            }
                            injections.add(b.length());
                            b.append(ch);
                    }
            }
        }

        if (b.length() > 0) {
            handleStatement(b, injections);
        }
    }

    //Just a small optimization to minimize matcher creation time - please note that these fields are not thread-safe
    private final Matcher emptyMatcher = EMPTY_PTR.matcher("");
    private final Matcher m = PropertiesSubstitutor.PROP_PTR.matcher("");
    private final Matcher extM = PropertiesSubstitutor.EXPR_PTR.matcher("");
    private void handleStatement(final StringBuilder sql,
                                 final List<Integer> injections) {
        if (emptyMatcher.reset(sql).matches()) {
            return;
        }

        if (injections!=null && !injections.isEmpty()) {
            m.reset(sql);
            extM.reset(sql);

            StringBuilder res = new StringBuilder(sql.length());
            int lastPos = 0;

            for (Integer index : injections) {
                int ind = index+1;
                if (m.find(ind) && (m.start() == ind)) { //property reference
                    res.append(sql.substring(lastPos, m.start()-1));
                    lastPos = m.end();
                    res.append(handleParameter(m.group(1), false));
                } else if (extM.find(ind) && (extM.start() == ind)) { //expression
                    res.append(sql.substring(lastPos, extM.start()-1));
                    lastPos = extM.end();
                    res.append(handleParameter(extM.group(1), true));
                }
            }

            if (lastPos < sql.length()) { //Add right side
                res.append(sql.substring(lastPos, sql.length()));
            }
            statementParsed(res.toString());
        } else {
            statementParsed(sql.toString());
        }


    }

    /**
     * Called when parameter is encountered in SQL.
     *
     * @param name       parameter name or expression
     * @param expression true if specified name is an expression, not a simple property reference
     * @return substituion string.
     */
    protected String handleParameter(final String name, final boolean expression) {
        return expression ? ("${" + name + '}') : ("$" + name);
    }

    /**
     * Invoked when SQL statement has been processed and all expressions handled.
     *
     * @param sql content of the preprocessed statement.
     */
    protected void statementParsed(final String sql) {
    }

}
