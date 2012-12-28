/*
 * Copyright 2006-2012 The Scriptella Project Team.
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
package scriptella.driver.text;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * Pre Java SE 6 adapters for {@link System#in} and {@link System#out}
 * <p>TODO: Move this class to a spi.support.text package.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConsoleAdapters {


    /**
     * Returns the reader for System.in.
     *
     * @param charsetName charset name, can be null.
     * @return System.in reader.
     * @throws UnsupportedEncodingException if specified charset is unsupported
     */
    public static Reader getConsoleReader(String charsetName) throws UnsupportedEncodingException {
        return charsetName == null ? new SystemInReader() : new SystemInReader(charsetName);
    }

    /**
     * Returns the writer for System.out.
     *
     * @param charsetName charset name, can be null.
     * @return System.out writer.
     * @throws UnsupportedEncodingException if specified charset is unsupported
     */
    public static Writer getConsoleWriter(String charsetName) throws UnsupportedEncodingException {
        return charsetName == null ? new SystemOutWriter() : new SystemOutWriter(charsetName);
    }


    /**
     * A simple {@link java.io.Reader} adapter for {@link java.io.InputStream}.
     */
    static class SystemInReader extends InputStreamReader {

        public SystemInReader() {
            super(System.in);
        }

        public SystemInReader(String charsetName) throws UnsupportedEncodingException {
            super(System.in, charsetName);
        }

        public void close() throws IOException {
        }
    }

    /**
     * A simple {@link java.io.Writer} adapter for {@link java.io.PrintStream}.
     */
    static class SystemOutWriter extends OutputStreamWriter {


        public SystemOutWriter() throws UnsupportedEncodingException {
            super(System.out);
        }

        public SystemOutWriter(String charsetName) throws UnsupportedEncodingException {
            super(System.out, charsetName);
        }

        public void close() throws IOException {
            flush();
        }
    }
}
