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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

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
    static final long MAX_LENGTH = 1024*10000; //10Mb

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
     * @param reader reader to load content from. Closed at the end of the operation.
     * @param maxLength max number of characters to read before throwing a Content Too Long Exception.
     * @return string representation of reader content.
     */
    public static String toString(final Reader reader, final long maxLength) throws IOException {
        char cb[] = new char[4096];
        StringBuilder sb = new StringBuilder(cb.length*2);
        long len = 0;

        try {
            for (int c; (c = reader.read(cb)) > 0;) {
                len+=c;
                if (len>maxLength) {
                    throw new IOException("Content too long to fit in memory");
                }
                sb.append(cb, 0, c);
            }
        } finally {
            closeSilently(reader);
        }

        return sb.toString();
    }

    /**
     * Loads an input stream content into a byte array.
     * @param is stream to load. Closed at the end of the operation.
     * @return stream bytes
     * @throws IOException if I/O error occurs or stream length exceeds the {@link #MAX_LENGTH}.
     */
    public static byte[] toByteArray(InputStream is) throws IOException {
        return toByteArray(is, MAX_LENGTH);
    }

    /**
     * Loads an input stream content into a byte array.
     * @param is stream to load. Closed at the end of the operation.
     * @param maxLength maxLength max number of bytes to read before throwing a Content Too Long Exception.
     * @return stream bytes
     * @throws IOException
     */
    public static byte[] toByteArray(InputStream is, long maxLength) throws IOException {
        byte b[] = new byte[4096];
        ByteArrayOutputStream os = new ByteArrayOutputStream(b.length*2);
        long len = 0;

        try {
            for (int n; (n = is.read(b)) > 0;) {
                len+=n;
                if (len>maxLength) {
                    throw new IOException("Content too long to fit in memory");
                }
                os.write(b,0,n);
            }
        } finally {
            closeSilently(is);
        }

        return os.toByteArray();
    }
}
