/*
 * Copyright 2006-2007 The Scriptella Project Team.
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

import scriptella.util.IOUtils;
import scriptella.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

/**
 * Factory for LOBs.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class Lobs {
    private Lobs() { //Singleton
    }

    /**
     * Create a new read-only BLOB for specified input stream.
     * <p>The stream will be lazily read into memory or saved on disk depending on its length.
     *
     * @param is input stream to create blob.
     * @return read-only Blob instance.
     */
    public static Blob newBlob(InputStream is) {
        return new ReadonlyBlob(is);
    }

    /**
     * Create a new read-only BLOB for specified input stream.
     * <p>The stream will be lazily read into memory or saved on disk depending on its length.
     *
     * @param is     input stream to create blob.
     * @param length input stream length.
     * @return read-only Blob instance.
     */
    public static Blob newBlob(InputStream is, long length) {
        return new ReadonlyBlob(is, length);
    }

    /**
     * Create a new read-only BLOB for specified URL.
     * <p>The URL content will be lazily fetched into memory or saved on disk depending on its length.
     *
     * @param url URL of the blob content.
     * @return read-only Blob instance.
     * @see #newBlob(java.io.InputStream)
     */
    public static Blob newBlob(URL url) {
        return new UrlBlob(url);
    }

    /**
     * Create a new read-only CLOB for specified reader.
     * <p>The reader will be lazily read into memory or saved on disk depending on its length.
     *
     * @param reader reader to create CLOB.
     * @return read-only Clob instance.
     */
    public static Clob newClob(Reader reader) {
        return new ReadonlyClob(reader);
    }

    /**
     * Create a new read-only CLOB for specified reader.
     * <p>The reader will be lazily read into memory or saved on disk depending on its length.
     *
     * @param reader reader to create CLOB.
     * @param length content length.
     * @return read-only Clob instance.
     */
    public static Clob newClob(Reader reader, long length) {
        return new ReadonlyClob(reader, length);
    }

    /**
     * Base class for LOBs.
     * <p>Defines a common functionality and helper methods.
     */
    static abstract class AbstractLob<T extends Closeable> implements Closeable {
        static int LOB_MAX_MEM = 100 * 1024; //100Kb
        protected File tmpFile;
        protected long length = -1;
        protected T source;

        /**
         * For custom instantiation.
         */
        protected AbstractLob() {
        }

        protected AbstractLob(T source) {
            if (source == null) {
                throw new IllegalArgumentException("Input source cannot be null");
            }
            this.source = source;
        }

        protected AbstractLob(T source, long length) {
            if (source == null) {
                throw new IllegalArgumentException("Input source cannot be null");
            }
            if (length < 0) {
                throw new IllegalArgumentException("Input source length cannot be negative");
            }
            this.source = source;
            this.length = length;
        }

        public void close() {
            if (tmpFile != null) {
                tmpFile.delete();
                tmpFile = null;
            }
        }

        /**
         * Read bytes/chars from source stream/reader.
         *
         * @param inmemory true if data should be read into a memory.
         * @return number of bytes/chars read.
         * @throws IOException
         */
        protected abstract int read(boolean inmemory) throws IOException;

        /**
         * Flush the data stored in a memory to disk.
         * <p>This method is invoked 0 or 1 time.
         *
         * @throws IOException
         */
        protected abstract void flushToDisk() throws IOException;

        protected abstract void onInitComplete();

        /**
         * Performs a LOB initialization in following steps:
         * <ul>
         * <li>Reads content into memory until the size exceeds {@link #LOB_MAX_MEM}.
         * <li>{@link #flushToDisk() Flushes} memory conent to disk.
         * <li>Copy the left bytes to disk.
         * </ul>
         */
        protected void init() {
            if (source == null && length >= 0) {
                return;
            }

            int n;
            try {
                for (length = 0; (n = read(true)) >= 0;) {
                    length += n;
                    if (length > LOB_MAX_MEM) {
                        break;
                    }
                }
                if (n >= 0) {
                    flushToDisk();
                    for (; (n = read(false)) >= 0;) {
                        length += n;
                    }
                }
            } catch (IOException e) {
                throw new JdbcException("Cannot initialize temprorary file storage", e);
            } finally {
                IOUtils.closeSilently(source);
                source = null;
                onInitComplete();
            }
        }

        /**
         * Returns true if this LOB is stored in memory.
         */
        public boolean isInMemory() {
            return tmpFile == null;
        }

        /**
         * Creates a temprorary file.
         *
         * @return output stream for temprorary file created.
         * @throws IOException if I/O error occurs.
         */
        protected OutputStream createTempFile() throws IOException {
            tmpFile = File.createTempFile("blob_", null);
            tmpFile.deleteOnExit();
            return new FileOutputStream(tmpFile);
        }

        /**
         * Returns an input stream for temprorary file.
         */
        protected InputStream getTempFileInputStream() {
            if (tmpFile == null) {
                throw new IllegalStateException("Internal error - temprorary file was not created");
            }
            try {
                return new FileInputStream(tmpFile);
            } catch (FileNotFoundException e) {
                throw new JdbcException("Cannot open stream - temprorary file has been removed", e);
            }
        }

        /**
         * Returns the length of this LOB.
         */
        public long length() {
            if (length < 0) {
                init();
            }
            return length;

        }

    }

    /**
     * Represents a BLOB located at specified URL.
     */
    static class UrlBlob extends ReadonlyBlob {
        private URL url;

        public UrlBlob(URL url) {
            if (url == null) {
                throw new IllegalArgumentException("URL cannot be null");
            }
            this.url = url;
        }

        URL getUrl() {
            return url;
        }

        @Override
        protected void init() {
            if (length < 0) {
                try {
                    final URLConnection c = url.openConnection();
                    source = c.getInputStream();
                    length = c.getContentLength();
                    if (length < 0) { //if length is undefined - fetch the url
                        super.init();
                    }
                } catch (IOException e) {
                    throw new JdbcException("Unable to read content for file " + url +
                            ": " + e.getMessage(), e);
                }
            }
        }

        @Override
        public InputStream getBinaryStream() {
            InputStream src = source;
            if (src != null) {
                source = null;
                return src;
            } else {
                length = -1;
                init();
                src = source;
                source = null;
                return (src == null) ? super.getBinaryStream() : src;
            }
        }
    }

    /**
     * Readonly implementation of {@link java.sql.Blob}.
     *
     * @author Fyodor Kupolov
     * @version 1.0
     */
    static class ReadonlyBlob extends AbstractLob<InputStream> implements Blob {
        private byte[] bytes;
        private byte[] buffer = new byte[8192];
        private ByteArrayOutputStream memStream;
        private OutputStream diskStream;

        /**
         * For custom instantion.
         */
        protected ReadonlyBlob() {
        }

        public ReadonlyBlob(InputStream source) {
            super(source);
        }

        public ReadonlyBlob(InputStream source, long length) {
            super(source, length);
        }

        protected int read(boolean inmemory) throws IOException {
            int n = source.read(buffer);
            if (n > 0) {
                if (inmemory) {
                    if (memStream == null) {
                        memStream = new ByteArrayOutputStream(n);
                    }
                    memStream.write(buffer, 0, n);
                } else {
                    diskStream.write(buffer, 0, n);
                }
            }
            return n;
        }

        protected void flushToDisk() throws IOException {
            diskStream = createTempFile();
            memStream.writeTo(diskStream);
            memStream = null;
        }

        protected void onInitComplete() {
            if (diskStream != null) {
                IOUtils.closeSilently(diskStream);
                diskStream = null;
            }
            if (memStream != null) {
                bytes = memStream.toByteArray();
                memStream = null;
            }
        }

        public InputStream getBinaryStream() {
            init();
            if (isInMemory()) {
                return new ByteArrayInputStream(bytes);
            } else {
                return getTempFileInputStream();
            }
        }

        public void close() {
            super.close();
            if (bytes != null) {
                bytes = null;
            }
        }

        public String toString() {
            try {
                return "BLOB: " + StringUtils.consoleFormat(
                        new String(IOUtils.toByteArray(getBinaryStream(), 1024)));
            } catch (Exception e) {
                return "BLOB: " + e;
            }
        }

        //--------------- Unsupported methods
        public byte[] getBytes(long pos, int length) throws SQLException {
            throw new SQLException("Unsupported operation"); //Due to performance reasons
        }

        public long position(byte pattern[], long start) throws SQLException {
            throw new SQLException("Unsupported operation");
        }

        public long position(Blob pattern, long start) throws SQLException {
            throw new SQLException("Unsupported operation");
        }

        public int setBytes(long pos, byte[] bytes) throws SQLException {
            throw new SQLException("Unsupported operation");
        }

        public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
            throw new SQLException("Unsupported operation");
        }

        public OutputStream setBinaryStream(long pos) throws SQLException {
            throw new SQLException("Unsupported operation");
        }

        public void truncate(long len) throws SQLException {
            throw new SQLException("Unsupported operation");
        }

    }

    /**
     * Represents a read-only CLOB.
     */
    static class ReadonlyClob extends AbstractLob<Reader> implements Clob {
        private String string;
        private char[] buffer = new char[8192];
        private StringBuilder mem;
        private Writer diskWriter;

        public ReadonlyClob(Reader source) {
            super(source);
        }

        public ReadonlyClob(Reader source, long length) {
            super(source, length);
        }

        protected int read(boolean inmemory) throws IOException {
            int n = source.read(buffer);
            if (n > 0) {
                if (inmemory) {
                    if (mem == null) {
                        mem = new StringBuilder(n);
                    }
                    mem.append(buffer, 0, n);
                } else {
                    diskWriter.write(buffer, 0, n);
                }
            }
            return n;
        }

        protected void flushToDisk() throws IOException {
            diskWriter = new OutputStreamWriter(createTempFile(), "UTF-8");
            diskWriter.append(mem);
            mem = null;
        }

        protected void onInitComplete() {
            if (diskWriter != null) {
                IOUtils.closeSilently(diskWriter);
                diskWriter = null;
            }
            if (mem != null) {
                string = mem.toString();
                mem = null;
            }
        }

        public Reader getCharacterStream() {
            init();
            if (isInMemory()) {
                return new StringReader(string);
            } else {
                try {
                    return new InputStreamReader(getTempFileInputStream(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e); //should never happen
                }
            }
        }

        /**
         * For debug purposes.
         */
        public String toString() {
            try {
                return "CLOB: " + StringUtils.consoleFormat(IOUtils.toString(getCharacterStream(), 1024));
            } catch (Exception e) {
                return "CLOB: " + e;
            }
        }

        //--------------- Unsupported methods
        public String getSubString(long pos, int length) throws SQLException {
            throw new SQLException("Unsupported operation");
        }


        public InputStream getAsciiStream() throws SQLException {
            throw new SQLException("Unsupported operation");
        }

        public long position(String searchstr, long start) throws SQLException {
            throw new SQLException("Unsupported operation");
        }

        public long position(Clob searchstr, long start) throws SQLException {
            throw new SQLException("Unsupported operation");
        }

        public int setString(long pos, String str) throws SQLException {
            throw new SQLException("Unsupported operation");
        }

        public int setString(long pos, String str, int offset, int len) throws SQLException {
            throw new SQLException("Unsupported operation");
        }

        public OutputStream setAsciiStream(long pos) throws SQLException {
            throw new SQLException("Unsupported operation");
        }

        public Writer setCharacterStream(long pos) throws SQLException {
            throw new SQLException("Unsupported operation");
        }

        public void truncate(long len) throws SQLException {
            throw new SQLException("Unsupported operation");
        }
    }

}
