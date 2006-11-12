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
package scriptella.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;


/**
 * TODO: Refactor this class to support unicode convertion similar to native2ascii works
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class ReaderInputStream extends InputStream {
    private Reader reader;

    public ReaderInputStream(Reader reader) {
        this.reader = reader;
    }

    public int read() throws IOException {
        return reader.read();
    }

    private char[] tmpBuf;//used to minimize memory allocations

    public int read(final byte b[], final int off, final int len)
            throws IOException {
        char[] c;
        if (tmpBuf != null && tmpBuf.length >= len) {
            c = tmpBuf;
        } else {
            c = new char[len];
        }

        int n = reader.read(c, 0, len);
        tmpBuf = c;

        if (n > 0) {
            for (int i = 0; i < n; i++) {
                b[off + i] = (byte) c[i];
            }
        }

        return n;
    }

    public void close() throws IOException {
        tmpBuf = null;
        reader.close();
    }
}
