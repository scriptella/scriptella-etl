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
package scriptella.jdbc;

import scriptella.configuration.ConfigurationException;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.util.ExceptionUtils;
import scriptella.util.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.regex.Matcher;


/**
 * Customizable SQL parser.
 * <p><u>Supported extensions</u>
 * The parser supports extensions described in {@link PropertiesSubstitutor}.<br/>
 * Additionally <b>?</b> prefix is used for expressions which should be injected as prepared statement parameters.
 * <p>Example:
 * <pre><code>
 * var=_name
 * id=11
 * --------------------------------------
 * select * FROM table${var} where id=?id
 *      --- is transformed to ---
 * select * FROM table_name where id=?  where statement parameter has value of 11
 * </code></pre>
 * <p><u>Notes:</u><br>
 * SQL quoted expressions and comments are not substituted. Example:
 * <pre><code>
 * SELECT * FROM "Table" WHERE NAME="John${prop}" and SURNAME=?surname; --only SURNAME is handled
 * </code></pre>
 * These extensions are handled by subclasses in {@link #handleParameter(String, boolean, boolean)} method.
 *
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SQLParserBase {

    /**
     * Parses SQL script.
     *
     * @param reader reader with SQL script.
     */
    public void parse(final Reader reader) {
        try {
            SqlTokenizer tok = new SqlTokenizer(reader);
            for (StringBuilder sb;(sb=tok.nextStatement())!=null;) {
                handleStatement(sb, tok.getInjections());
            }
        } catch (IOException e) {
            throw new ConfigurationException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                ExceptionUtils.ignoreThrowable(e);
            }
        }
    }

    private final Matcher m = PropertiesSubstitutor.PROP_PTR.matcher("");
    private final Matcher extM = PropertiesSubstitutor.EXPR_PTR.matcher("");
    private final StringBuilder tmpBuf = new StringBuilder();

    private void handleStatement(final StringBuilder sql,
                                 final List<Integer> injections) {
        if (StringUtils.isAsciiWhitespacesOnly(sql)) {
            return;
        }

        if (injections != null && !injections.isEmpty()) {
            m.reset(sql);
            extM.reset(sql);

            tmpBuf.setLength(0); //clearing the string builder
            int lastPos = 0;

            for (Integer index : injections) {
                int ind = index + 1;
                Matcher found = null;
                boolean expr = false;
                if (m.find(ind) && (m.start() == ind)) { //property reference
                    found = m;

                } else if (extM.find(ind) && (extM.start() == ind)) { //expression
                    found = extM;
                    expr = true;
                }
                if (found != null) {
                    int exprStart = ind - 1;
                    //? - jdbcParam, $ - insert value as text
                    boolean jdbcParam = sql.charAt(exprStart) == '?';
                    tmpBuf.append(sql.substring(lastPos, exprStart));
                    lastPos = found.end();
                    tmpBuf.append(handleParameter(found.group(1), expr, jdbcParam));
                }

            }

            if (lastPos < sql.length()) { //Add right side
                tmpBuf.append(sql.substring(lastPos, sql.length()));
            }
            statementParsed(tmpBuf.toString());
        } else {
            statementParsed(sql.toString());
        }


    }

    /**
     * Called when parameter is encountered in SQL.
     *
     * @param name       parameter name or expression
     * @param expression true if specified name is an expression, not a simple property reference
     * @param jdbcParam  true if parameter value should be passed as prepared statement parameter. Othewise it's value should be inserted
     *                   into statement text.
     * @return substituion string.
     */
    protected String handleParameter(final String name, final boolean expression, final boolean jdbcParam) {
        return expression ? ((jdbcParam ? "?{" : "${") + name + '}') : ((jdbcParam ? "?{" : "$") + name);
    }

    /**
     * Invoked when SQL statement has been processed and all expressions handled.
     *
     * @param sql content of the preprocessed statement.
     */
    protected void statementParsed(final String sql) {
    }

}
