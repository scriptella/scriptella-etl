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

import scriptella.util.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizer for SQL statements.
 * <p>This class splits sql statements using a specifed
 * {@link #setSeparator(String) separator string}.
 * <p>The ? injections in quoted literals and comments are skipped.
 * The $ substitutions are skipped only in comments.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SqlTokenizer implements Closeable {
    private final Reader reader;
    private final List<Integer> injections = new ArrayList<Integer>();
    private final StringBuilder sb = new StringBuilder(80);
    private char[] separator = DEFAULT_SEPARATOR;
    private boolean separatorOnSingleLine;
    private boolean trim;
    private SeparatorMatcher separatorMatcher = new SeparatorMatcher();
    private static final char[] DEFAULT_SEPARATOR = ";".toCharArray();

    public SqlTokenizer(Reader reader) {
        this.reader = reader;
    }

    public SqlTokenizer(Reader reader, String separator, boolean separatorOnSingleLine) {
        this.reader = reader;
        setSeparator(separator);
        setSeparatorOnSingleLine(separatorOnSingleLine);
    }

    private int position;
    private char previousChar = (char) -1;
    private char currentChar;

    private int lastLineStart;


    /**
     * Parses the following SQL statement from reader.
     *
     * @return parsed SQL statement
     * @throws IOException if I/O exception occurs.
     */
    public StringBuilder nextStatement() throws IOException {
        sb.setLength(0);
        injections.clear();
        int n;
        final boolean newLineMode = separatorOnSingleLine; //make a local copy for performance reasons
        final boolean defaultMode = !newLineMode;
        boolean whitespacesOnly = true;
        final char sep0 = separator[0];
        lastLineStart = 0;


        previousChar = (char) -1;
        for (position = 0; (n = reader.read()) >= 0; position++) {
            currentChar = (char) n;
            sb.append(currentChar);
            //Checking separator substring
            if ((currentChar == sep0) && //if matched a first separator char
                    //and no whitespaces in new line mode or not a new line mode
                    ((newLineMode && whitespacesOnly) || defaultMode)) {
                if (separatorMatcher.matches()) {  //try to match the whole string
                    return sb;
                }
            }
            if (newLineMode && currentChar > 32) {
                whitespacesOnly = false;
            }
            switch (currentChar) {
                case '-':
                    if (previousChar == '-') { //Comment
                        seekEol();
                        whitespacesOnly = true;
                        lastLineStart = position + 1;
                    }
                    break;
                case '/':
                    if (previousChar == '/') { //Comment
                        seekEol();
                    }
                    break;
                case '*':
                    if (previousChar == '/') {
                        seekEndCStyleComment();
                    }
                    break;
                case '"':
                    seekQuote('\"');
                    break;
                case '\'':
                    seekQuote('\'');
                    break;
                case '?':
                case '$':
                    injections.add(position);
                    break;
                case '\r':
                case '\n': //new line started
                    whitespacesOnly = true;
                    lastLineStart = position + 1;
                    break;
            }

            previousChar = currentChar;
        }
        if (sb.length() > 0) {
            return sb;
        } else return n >= 0 ? sb : null;
    }

    /**
     * @return injections for the last returned statement.
     */
    public List<Integer> getInjections() {
        return injections;
    }

    private void seekQuote(char q) throws IOException {
        position++;
        for (int n; (n = reader.read()) >= 0; position++) {
            sb.append((char) n);
            if ('$' == n) { //$ expressions are substituted in quotes
                injections.add(position);
            } else if (q == n) {  //quote
                return;
            }
        }
    }

    private void seekEol() throws IOException {
        position++;
        for (int n; (n = reader.read()) >= 0; position++) {
            sb.append((char) n);
            if ('\r' == n || '\n' == n) {  //EOL
                return;
            }
        }
    }

    private void seekEndCStyleComment() throws IOException {
        position++;
        for (int n; (n = reader.read()) >= 0; position++) {
            sb.append((char) n);
            if ('/' == n && previousChar == '*') {  // / * Comment
                previousChar = (char) n;
                return;
            }
            previousChar = (char) n;
        }
    }

    public String getSeparator() {
        return new String(separator).intern();
    }

    /**
     * Sets statements separator.
     *
     * @param separator statements separator. Default value is &quot;;&quot;
     */
    public void setSeparator(String separator) {
        if (StringUtils.isEmpty(separator)) {
            throw new IllegalArgumentException("separator string cannot be empty");
        }
        this.separator = separator.toCharArray();
    }

    public boolean isSeparatorOnSingleLine() {
        return separatorOnSingleLine;
    }

    public boolean isTrim() {
        return trim;
    }

    /**
     * Sets line trimming option.
     *
     * @param trim true if extra ASCII whitespaces should be removed from a line.
     */
    public void setTrim(boolean trim) {
        this.trim = trim;
    }

    /**
     * Sets the separator mode.
     *
     * @param separatorOnSingleLine true if {@link #separator} must be on a single line.
     */
    public void setSeparatorOnSingleLine(boolean separatorOnSingleLine) {
        this.separatorOnSingleLine = separatorOnSingleLine;
    }

    public static void main(String[] args) throws IOException {
        SqlTokenizer tok = new SqlTokenizer(new StringReader("he?llo/*t?ttt*/oo; --comment; text\n" +
                ";text${dd}2\"dd?dd\""));


        for (StringBuilder sb; (sb = tok.nextStatement()) != null;) {
            System.out.println("sb = " + sb);
            List<Integer> inj = tok.getInjections();
            System.out.println("inj = " + inj);

        }

    }

    private class SeparatorMatcher {
        private boolean matches() throws IOException {
            final int separatorLength = separator.length;
            for (int j = 1, n; (j < separatorLength) && (n = reader.read()) >= 0; j++) {
                position++;
                previousChar = currentChar;
                currentChar = (char) n;
                sb.append(currentChar);
                if (separator[j] != n) {
                    return false;
                }

            }
            if (!separatorOnSingleLine) {
                final int len = sb.length();
                sb.delete(len - separatorLength, len);
                return true;
            } else {
                for (int n; (n = reader.read()) >= 0;) {
                    position++;
                    previousChar = currentChar;
                    currentChar = (char) n;
                    sb.append(currentChar);
                    if (n > 32) {
                        return false;
                    } else if (n == '\r' || n == '\n') {
                        break;
                    }
                }
                sb.delete(lastLineStart, sb.length());
                return true;
            }
        }
    }

    public void close() throws IOException {
        reader.close();
    }


}

