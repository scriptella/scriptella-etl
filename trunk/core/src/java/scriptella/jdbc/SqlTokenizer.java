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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizer for SQL statements.
 * <p>This class splits sql statements using a specifed
 * {@link #setSeparator(char) separator char}.
 * <p>The ? injections in quoted literals and comments are skipped.
 * The $ substitutions are skipped only in comments.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SqlTokenizer {
    private final Reader reader;
    private final List<Integer> injections = new ArrayList<Integer>();
    private final StringBuilder sb = new StringBuilder(80);
    private char separator = ';';

    public SqlTokenizer(Reader reader) {
        this.reader = reader;
    }


    /**
     * Parses the following SQL statement from reader.
     *
     * @return parsed SQL statement
     * @throws IOException if I/O exception occurs.
     */
    public StringBuilder nextStatement() throws IOException {
        sb.setLength(0);
        injections.clear();
        char prevChar = (char) -1;
        int n;
        for (int i = 0; (n = reader.read()) >= 0; i++) {
            char c = (char) n;
            if (c==separator) {
                return sb;
            }
            sb.append(c);
            switch (c) {
                case '-':
                    if (prevChar == c) { //Comment
                        prevChar=(char) -1;
                        i+=seekEol(sb);
                        continue;
                    }
                    break;
                case '/':
                    if (prevChar == '/') { //Comment
                        prevChar=(char) -1;
                        i+=seekEol(sb);
                        continue;
                    }
                    break;
                case '*':
                    if (prevChar == '/') {
                        i+=seekEndCStyleComment(sb);
                        prevChar=(char) -1;
                        continue;
                    }
                    break;
                case '"':
                    i=seekQuote(sb, '\"', i);
                    prevChar=(char) -1;
                    continue;
                case '\'':
                    i=seekQuote(sb, '\'', i);
                    prevChar=(char) -1;
                    continue;
                case '?':
                case '$':
                    injections.add(i);
                    break;
            }

            prevChar = c;
        }
        if (sb.length()>0) {
            return sb;
        } else return n>=0?sb:null;
    }

    /**
     * @return injections for the last returned statement.
     */
    public List<Integer> getInjections() {
        return injections;
    }
    private int seekQuote(StringBuilder sb, char q, int pos) throws IOException {
        int i=pos+1;
        for (int n; (n = reader.read()) >= 0;i++) {
            sb.append((char) n);
            if ('$'==n) { //$ expressions are substituted in quotes
                injections.add(i);
            } else if (q == n) {  //quote
                return i;
            }
        }
        return i;

    }

    private int seekEol(StringBuilder sb) throws IOException {
        int i=0;
        for (int n; (n = reader.read()) >= 0; i++) {
            sb.append((char) n);
            if ('\r' == n || '\n' == n) {  //EOL
                return i+1;
            }
        }
        return i;
    }

    private int seekEndCStyleComment(StringBuilder sb) throws IOException {
        char prevChar = (char) -1;
        int i=0;
        for (int n; (n = reader.read()) >= 0; i++) {
            sb.append((char) n);
            if ('/' == n && prevChar == '*') {  // / * Comment
                return i+1;
            }
            prevChar = (char) n;
        }
        return i;
    }

    public char getSeparator() {
        return separator;
    }

    /**
     * Sets statements separator.
     * @param separator statements separator. Default value is ';'
     */
    public void setSeparator(char separator) {
        this.separator = separator;
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

}

