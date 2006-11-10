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
import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizer for SQL statements.
 * <p>This class splits sql statements using a specifed
 * {@link #setSeparator(String) separator string}.
 * <p>The ? injections in quoted literals and comments are skipped.
 * The $ substitutions are skipped only in comments.
 * <p>This class became too complex and <b>needs to be refactored</b>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SqlTokenizer implements Closeable {
    private final ReaderWrapper reader;
    private final List<Integer> injections = new ArrayList<Integer>();
    private final StringBuilder sb = new StringBuilder(80);
    private char[] separator = DEFAULT_SEPARATOR;
    private boolean separatorOnSingleLine;
    private boolean keepFormat;
    private SeparatorMatcher separatorMatcher = new SeparatorMatcher();
    private static final char[] DEFAULT_SEPARATOR = ";".toCharArray();

    public SqlTokenizer(Reader reader) {
        this.reader = new ReaderWrapper(reader);
    }

    public SqlTokenizer(Reader reader, String separator, boolean separatorOnSingleLine, boolean keepFormat) {
        this.reader = new ReaderWrapper(reader);
        setKeepFormat(keepFormat);
        setSeparator(separator);
        setSeparatorOnSingleLine(separatorOnSingleLine);
    }

    private int position;
    private char previousChar;
    private char currentChar;

    private int lastLineStart;


    /**
     * Parses the following SQL statement from reader.
     *
     * @return parsed SQL statement
     * @throws IOException if I/O exception occurs
     */
    public StringBuilder nextStatement() throws IOException {
        sb.setLength(0);
        injections.clear();
        final boolean newLineMode = separatorOnSingleLine; //make a local copy for performance reasons
        final boolean defaultMode = !newLineMode;
        boolean whitespacesOnly = true;
        final char sep0 = separator[0];
        lastLineStart = 0;

        previousChar = (char) -1;
        int n;
        for (position = 0; (n = readNormalizedChar()) >= 0; position++) {
            currentChar=(char) n;
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
                        seekEndLineComment();
                        whitespacesOnly = true;
                    }
                    break;
                case '/':
                    if (previousChar == '/') { //Comment
                        seekEndLineComment();
                        whitespacesOnly = true;
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

    private int readNormalizedChar() throws IOException {
        for (int n;(n = reader.read())>=0;) {
            if (!keepFormat) {
                //Normalize char  \r,\n->\n  ; any whitespace transformed to space
                //If previous char was also a whitespace - this char is ignored
                if (n=='\n' || n=='\r') {
                    if (previousChar=='\n') {
                        continue;
                    }
                    n='\n';
                } else if (n <= ' ') {
                    if (previousChar == ' ' || previousChar == '\n') {
                        continue;
                    }
                    n = ' ';
                }
            }
            return n;
        }
        return -1;
    }

    /**
     * @return injections for the last returned statement.
     */
    public List<Integer> getInjections() {
        return injections;
    }

    private void seekQuote(char q) throws IOException {
        for (int n; (n = reader.read()) >= 0; ) {
            position++;
            sb.append((char) n);
            if ('$' == n) { //$ expressions are substituted in quotes
                injections.add(position);
            } else if (q == n) {  //quote
                return;
            }
        }
    }

    private void seekEndLineComment() throws IOException {
        if (!keepFormat) {
            position-=2;
            sb.setLength(position+1);
        }
        for (int n; (n = reader.read()) >= 0; ) {
            if (keepFormat) {
                position++;
                sb.append((char) n);
            }
            if ('\r' == n || '\n' == n) {  //EOL
                if (!keepFormat) {
                    position++;
                    sb.append('\n');
                    currentChar='\n';
                } else {
                    currentChar=(char) n;
                }
                lastLineStart = position + 1; //remember the new line position
                return;
            }
        }
    }

    /**
     * Seeks until end c-style comment * /.
     * If keepFormat=false, the comment string is not appended to the buffer.
     * @throws IOException if I/O error occurs
     */
    private void seekEndCStyleComment() throws IOException {
        boolean firstChar=true;
        boolean copyChars=true;
        for (int n; (n = reader.read()) >= 0; ) {
            //Oracle is extraordinary as always ;)
            //if oracle hint, i.e. /*+ and keepformat=false
            if (firstChar && !keepFormat && n!='+') {
                position-=2;
                sb.setLength(position+1);
                copyChars=false;
            }
            firstChar=false;

            if (copyChars) {
                position++;
                sb.append((char) n);
            }
            if ('/' == n && previousChar == '*') {  // / * Comment
                currentChar = (char) -1;
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

    /**
     * Sets the separator mode.
     *
     * @param separatorOnSingleLine true if {@link #separator} must be on a single line.
     */
    public void setSeparatorOnSingleLine(boolean separatorOnSingleLine) {
        this.separatorOnSingleLine = separatorOnSingleLine;
    }
    
    /**
     * Returns true if preserve comments and whitespaces. Default value is <b><code>false</code></b>
     * @return <tt>false</tt> by default
     */
    public boolean isKeepFormat() {
        return keepFormat;
    }

    /**
     * Keep original text format, i.e. preserve comments and whitespaces.
     * @param keepFormat true if comments/whitespaces should be preserved.
     */
    public void setKeepFormat(boolean keepFormat) {
        this.keepFormat = keepFormat;
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
                sb.setLength(len - separatorLength);
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
                sb.setLength(lastLineStart);
                return true;
            }
        }
    }


    /**
     * Unsynchronized buffered wrapper for a reader.
     * <p>Used for performance reasons to avoid multiple calls to underlying reader implementation,
     * this class is faster and lighter than BufferedReader for our case.
     */
    private static final class ReaderWrapper {
        /**
         * Size of internal buffer
         */
        private static final int BUF_SIZE=512; //optimal for small and huge scripts
        private char[] buf=new char[BUF_SIZE];
        private int bufSize;
        private int bufPos;
        private final Reader reader;

        ReaderWrapper(Reader reader) {
            this.reader = reader;
        }

        /**
         * Reads a character for reader.
         * <p>Internal bufferring is used for performance reasons.
         * @return a character read.
         * @throws IOException if I/O exception occurs
         */
        private int read() throws IOException {
            if (bufPos>=bufSize) { //buffer is empty
                bufSize=reader.read(buf, 0, BUF_SIZE);
                if (bufSize<0) {
                    return -1;
                }
                bufPos=0;
            }
            return buf[bufPos++];
        }

    }

    public void close() throws IOException {
        reader.reader.close();
    }
}

