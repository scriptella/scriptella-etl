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
package scriptella.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;

/**
 * I/O utility methods.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public final class IOUtils {
    //Singleton
    private IOUtils() {
    }

    /**
     * Default value of maximum stream size for arrays conversion
     */
    static final long MAX_LENGTH = 1024 * 10000; //10Mb

    /**
     * Silently closes data.
     *
     * @param closeable data to close
     */
    public static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                ExceptionUtils.ignoreThrowable(e);
            }
        }
    }

    /**
     * Silently closes a collection of objects.
     * @param closeables iterable closeables. Null value allowed.
     * @see #closeSilently(java.io.Closeable)
     */
    public static void closeSilently(Iterable<? extends Closeable> closeables) {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                closeSilently(closeable);
            }
        }
    }


    /**
     * Loads a reader content into a string.
     *
     * @param reader reader to load content from. Closed at the end of the operation.
     * @return string representation of reader content.
     */
    public static String toString(Reader reader) throws IOException {
        return toString(reader, MAX_LENGTH);

    }

    /**
     * Loads a reader content into a string.
     *
     * @param reader    reader to load content from. Closed at the end of the operation.
     * @param maxLength max number of characters to read before throwing a Content Too Long Exception.
     * @return string representation of reader content.
     */
    public static String toString(final Reader reader, final long maxLength) throws IOException {
        char cb[] = new char[4096];
        StringBuilder sb = new StringBuilder(cb.length);
        long len = 0;

        try {
            for (int n; (n = reader.read(cb)) >= 0;) {
                len += n;
                if (len > maxLength) {
                    throw new IOException("Content too long to fit in memory");
                }
                sb.append(cb, 0, n);
            }
        } finally {
            closeSilently(reader);
        }

        return sb.toString();
    }

    /**
     * Loads an input stream content into a byte array.
     *
     * @param is stream to load. Closed at the end of the operation.
     * @return stream bytes
     * @throws IOException if I/O error occurs or stream length exceeds the {@link #MAX_LENGTH}.
     */
    public static byte[] toByteArray(InputStream is) throws IOException {
        return toByteArray(is, MAX_LENGTH);
    }

    /**
     * Loads an input stream content into a byte array.
     *
     * @param is        stream to load. Closed at the end of the operation.
     * @param maxLength maxLength max number of bytes to read before throwing a Content Too Long Exception.
     * @return stream bytes
     * @throws IOException
     */
    public static byte[] toByteArray(InputStream is, long maxLength) throws IOException {
        byte b[] = new byte[4096];
        ByteArrayOutputStream os = new ByteArrayOutputStream(b.length);
        long len = 0;

        try {
            for (int n; (n = is.read(b)) >= 0;) {
                len += n;
                if (len > maxLength) {
                    throw new IOException("Content too long to fit in memory");
                }
                os.write(b, 0, n);
            }
        } finally {
            closeSilently(is);
        }

        return os.toByteArray();
    }

    /**
     * Opens output stream for specified URL.
     * <p>This method is a helper for url.openConnection().getOutputStream().
     * Additionally a file: URLs are supported,
     * see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4191800">
     * FileURLConnection doesn't implement getOutputStream()</a>
     *
     * @param url URL to open an output stream.
     * @return output stream for URL.
     * @throws IOException if an I/O error occurs while creating the output stream.
     */
    public static OutputStream getOutputStream(final URL url) throws IOException {
        if ("file".equals(url.getProtocol())) {
            return new FileOutputStream(url.getFile());
        } else {
            return url.openConnection().getOutputStream();
        }

    }

    /**
     * @return buffered reader for specified input stream.
     * @see #getReader(java.io.InputStream, String, boolean)
     *
     */
    public static Reader getReader(final InputStream is, final String enc) throws UnsupportedEncodingException {
        return getReader(is, enc, true);
    }

    /**
     * Returns reader for specified input stream and charset name.
     * @param is source input stream.
     * @param enc charset name, null means default.
     * @param buffered true if buffered reader should be used.
     * @return reader for inputstream.
     * @throws UnsupportedEncodingException  If the named charset is not supported
     */
    public static Reader getReader(final InputStream is, final String enc, final boolean buffered) throws UnsupportedEncodingException {
        Reader r = enc == null ? new InputStreamReader(is) : new InputStreamReader(is, enc);
        return buffered ? new BufferedReader(r) : r;
    }

    /**
     * Optionally makes a buffered reader from the specified one.
     * <p>If specified reader is buffered the object is returned unchanged.
     * @param reader reader to convert.
     * @return buffered reader.
     */
    public static BufferedReader asBuffered(Reader reader) {
        if (reader==null) {
            throw new IllegalArgumentException("Reader cannot be null");
        }
        return (reader instanceof BufferedReader?(BufferedReader)reader:new BufferedReader(reader));
    }

    /**
     * Optionally makes a buffered writer from the specified one.
     * <p>If specified writer is buffered the object is returned unchanged.
     * @param writer writer to convert.
     * @return buffered writer.
     */
    public static BufferedWriter asBuffered(Writer writer) {
        if (writer==null) {
            throw new IllegalArgumentException("Writer cannot be null");
        }
        return (writer instanceof BufferedWriter?(BufferedWriter)writer:new BufferedWriter(writer));
    }


    /**
     * @return buffered writer for specified output stream.
     * @see #getWriter(java.io.OutputStream, String, boolean)
     *
     */
    public static Writer getWriter(final OutputStream os, final String enc) throws IOException {
        return getWriter(os, enc, true);
    }

    /**
     * Returns writer for specified output stream and charset name.
     * @param os source output stream.
     * @param enc charset name, null means default.
     * @param buffered true if buffered reader should be used.
     * @return reader for inputstream.
     * @throws UnsupportedEncodingException  If the named charset is not supported
     */
    public static Writer getWriter(final OutputStream os, final String enc, final boolean buffered) throws IOException {
        Writer w = enc == null ? new OutputStreamWriter(os) : new OutputStreamWriter(os, enc);
        return buffered ? new BufferedWriter(w) : w;
    }

}
