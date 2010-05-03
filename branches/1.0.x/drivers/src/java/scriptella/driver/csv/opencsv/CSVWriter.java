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
package scriptella.driver.csv.opencsv;

import java.io.IOException;
import java.io.Writer;
import java.io.Closeable;

/**
 * A very simple CSV writer released under a commercial-friendly license.
 *
 * @author Glen Smith
 * @author Fyodor Kupolov
 *
 */
public class CSVWriter implements Closeable {

    private Writer writer;

    private char separator;

    private char quotechar;

    private String lineEnd;

    /** The character used for escaping quotes. */
    public static final char ESCAPE_CHARACTER = '"';

    /** The default separator to use if none is supplied to the constructor. */
    public static final char DEFAULT_SEPARATOR = ',';

    /**
     * The default quote character to use if none is supplied to the
     * constructor.
     */
    public static final char DEFAULT_QUOTE_CHARACTER = '"';

    /** The quote constant to use when you wish to suppress all quoting. */
    public static final char NO_QUOTE_CHARACTER = '\u0000';

    /** Default line terminator uses platform encoding. */
    public static final String DEFAULT_LINE_END = "\n";

    /**
     * Constructs CSVWriter using a comma for the separator.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     */
    public CSVWriter(Writer writer) {
        this(writer, DEFAULT_SEPARATOR);
    }

    /**
     * Constructs CSVWriter with supplied separator.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries.
     */
    public CSVWriter(Writer writer, char separator) {
        this(writer, separator, DEFAULT_QUOTE_CHARACTER);
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     */
    public CSVWriter(Writer writer, char separator, char quotechar) {
    	this(writer, separator, quotechar, "\n");
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     * @param lineEnd
     * 			  the line feed terminator to use
     */
    public CSVWriter(Writer writer, char separator, char quotechar, String lineEnd) {
        this.writer = writer;
        this.separator = separator;
        this.quotechar = quotechar;
        this.lineEnd = lineEnd;
    }


    /**
     * Writes the next line to the file.
     *
     * @param nextLine
     *            a string array with each comma-separated element as a separate
     *            entry.
     * @throws java.io.IOException if I/O error occurs
     */
    public void writeNext(String[] nextLine) throws IOException {
        final int colCount = nextLine.length;
        for (int i = 0; i < colCount; i++) {
            if (i != 0) {
                writer.append(separator);
            }

            String nextElement = nextLine[i];
            if (nextElement == null)
                continue;
            if (quotechar != NO_QUOTE_CHARACTER)
            	writer.append(quotechar);
            final int length = nextElement.length(); //Kupolov: use local variable in a loop
            for (int j = 0; j < length; j++) {
                char nextChar = nextElement.charAt(j);
                if (nextChar == quotechar) {
                    writer.append(ESCAPE_CHARACTER).append(nextChar);
                } else if (nextChar == ESCAPE_CHARACTER) {
                    writer.append(ESCAPE_CHARACTER).append(nextChar);
                } else {
                    writer.append(nextChar);
                }
            }
            if (quotechar != NO_QUOTE_CHARACTER)
            	writer.append(quotechar);
        }

        writer.append(lineEnd);

    }

    /**
     * Close the underlying stream writer flushing any buffered content.
     *
     * @throws java.io.IOException if bad things happen
     *
     */
    public void close() throws IOException {
        writer.flush();
        writer.close();
    }

}
