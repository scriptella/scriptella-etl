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
package scriptella.expression;

import scriptella.core.RuntimeIOException;
import scriptella.util.IOUtils;
import scriptella.util.StringUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An Iterator over the lines in a Reader, additionally properties substitution is performed.
 * <p>This class change the contract of {@link #hasNext()} method by
 * throwing {@link scriptella.core.RuntimeIOException} on {@link IOException}.
 * <p>Decorators should override {@link #format(String)} method.
 * <p>This class is not threadsafe.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 * @see PropertiesSubstitutor
 */
public class LineIterator implements Iterator<String>, Closeable {
    private PropertiesSubstitutor substitutor;
    private BufferedReader reader;
    private boolean trimLines;
    private String line;

    public LineIterator(Reader reader) {
        this(reader, null, false);
    }


    public LineIterator(Reader reader, PropertiesSubstitutor substitutor) {
        this(reader, substitutor, false);
    }

    /**
     * Constructs iterator.
     *
     * @param reader      reader to iterate.
     * @param substitutor substitutor to use to expand properties or null to disable substitution.
     * @param trimLines   true if the returned lines should be trimmed.
     */
    public LineIterator(Reader reader, PropertiesSubstitutor substitutor, boolean trimLines) {
        if (reader == null) {
            throw new IllegalArgumentException("reader cannot be null");
        }
        this.reader = IOUtils.asBuffered(reader);
        this.substitutor = substitutor;
        this.trimLines = trimLines;
    }

    /**
     * @return true if a line is available for reading by {@link #next()}
     * @throws RuntimeIOException if IO error occurs.
     */
    public boolean hasNext() throws RuntimeIOException {
        if (reader == null) {
            return false;
        }
        if (line == null) {
            try {
                line = format(reader.readLine());
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        }
        return line != null;
    }

    /**
     * Applies additional formatting to the line read.
     * <p>May be overriden by decorators.
     * @param line line of text, nulls allowed.
     * @return formatted line.
     */
    protected String format(String line) {
        if (StringUtils.isEmpty(line)) {
            return line;
        }
        if (trimLines) {
            line = line.trim();
        }
        if (substitutor != null) {
            line = substitutor.substitute(line);
        }
        return line;
    }

    /**
     * Returns the next avalable line in a reader.
     *
     * @return the next avalable line in a reader.
     * @throws RuntimeIOException     if IO error occurs.
     * @throws NoSuchElementException if has no more elements.
     */
    public String next() throws RuntimeIOException, NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        final String res = line;
        line = null;
        return res;
    }


    /**
     * Skips N lines.
     * @param n number of lines to skip.
     * @return the actual number of lines skipped.
     */
    public int skip(int n) {
        for (int i = 0; i < n; i++) {
            if (!hasNext()) {
                return i;
            }
            next();
        }
        return n;
    }

    /**
     * Returns specified line or null if EOF occured.
     * 
     * @param n line number relative to the current line in the input. n>=0
     * @return line n.
     */
    public String getLineAt(int n) {
        skip(n);
        return hasNext() ? next() : null;
    }

    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("remove not supported by " + getClass().getName());
    }


    public void close() throws IOException {
        if (reader != null) {
            reader.close();
            reader = null;
        }
    }
}
