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
package scriptella.jdbc;

import scriptella.AbstractTestCase;
import scriptella.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Tests for {@link Lobs}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class LobsTest extends AbstractTestCase {
    private static final byte[] bsob=new byte[1024*10]; //small
    private static final byte[] blob=new byte[1024*110]; //large
    private static final String clob;
    private static final String csob;
    static {
        //Initializing blob/clob and small objects

        for (int i=0;i<blob.length;i++) {
            blob[i]= (byte) (0x30+i%10);
        }
        clob=new String(blob);
        for (int i=0;i<bsob.length;i++) {
            bsob[i]= (byte) (0x30+i%10);
        }
        csob=new String(bsob);

        //reading test properties containing a set of drivers

    }

    public void testBlob() throws IOException {
        //Test the BLOB 110K
        Lobs.ReadonlyBlob b = (Lobs.ReadonlyBlob) Lobs.newBlob(new ByteArrayInputStream(blob));
        assertNull(b.tmpFile);
        assertEquals(blob.length, b.length());
        File tmpFile = b.tmpFile;
        assertNotNull(tmpFile);
        assertTrue(tmpFile.exists()); //temp file should exist
        assertEquals(blob.length, tmpFile.length()); //and contain the same number of bytes as blob
        assertTrue(Arrays.equals(blob, IOUtils.toByteArray(b.getBinaryStream())));
        //obtain a new stream second time to check if fresh stream is created for each call
        assertTrue(Arrays.equals(blob, IOUtils.toByteArray(b.getBinaryStream())));

        b.close();
        assertFalse(tmpFile.exists()); //temp file should be deleted on close
        //Test the small object 10K
        b = (Lobs.ReadonlyBlob) Lobs.newBlob(new ByteArrayInputStream(bsob));
        assertNull(b.tmpFile);
        assertEquals(bsob.length, b.length());
        assertNull(b.tmpFile);
        assertTrue(Arrays.equals(bsob, IOUtils.toByteArray(b.getBinaryStream())));
        b.close(); //Just to make sure closing works (smoke test)
        //Now test the constructor with length
        b = (Lobs.ReadonlyBlob) Lobs.newBlob(new ByteArrayInputStream(blob), blob.length);
        assertNull(b.tmpFile);
        assertEquals(blob.length, b.length());
        assertNull(b.tmpFile); //length is known no need to read file
        assertTrue(Arrays.equals(blob, IOUtils.toByteArray(b.getBinaryStream())));
        tmpFile = b.tmpFile;
        assertNotNull(tmpFile); //temp file should be created
        b.close();

    }

    public void testUrlBlob() throws IOException, SQLException {
        testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                return new ByteArrayInputStream(bsob);
            }

            public OutputStream getOutputStream(final URL u) {
                throw new UnsupportedOperationException();
            }

            public int getContentLength(final URL u) {
                return bsob.length;
            }
        };
        Lobs.UrlBlob b= (Lobs.UrlBlob) Lobs.newBlob(new URL("tst://file"));
        assertTrue(Arrays.equals(bsob, IOUtils.toByteArray(b.getBinaryStream())));
        //Check again to receive a new stream
        assertTrue(Arrays.equals(bsob, IOUtils.toByteArray(b.getBinaryStream())));

        assertEquals(bsob.length, b.length());
        b.close();
        testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                return new ByteArrayInputStream(bsob);
            }

            public OutputStream getOutputStream(final URL u) {
                throw new UnsupportedOperationException();
            }

            public int getContentLength(final URL u) {
                return -1;
            }
        };
        b = (Lobs.UrlBlob) Lobs.newBlob(new URL("tst://file"));
        assertTrue(Arrays.equals(bsob, IOUtils.toByteArray(b.getBinaryStream())));
        //Check again to receive a new stream
        assertTrue(Arrays.equals(bsob, IOUtils.toByteArray(b.getBinaryStream())));
        assertEquals(bsob.length, b.length());
        b.close();
        assertNull(b.tmpFile);
    }

    public void testClob() throws IOException {
        //Test the CLOB 110K
        Lobs.ReadonlyClob c = (Lobs.ReadonlyClob) Lobs.newClob(new StringReader(clob));
        assertNull(c.tmpFile);
        assertEquals(clob.length(), c.length());
        assertNotNull(c.tmpFile);
        File tmp = c.tmpFile;
        assertEquals(clob, IOUtils.toString(c.getCharacterStream()));
        //obtain a new stream second time to check if fresh stream is created for each call
        assertEquals(clob, IOUtils.toString(c.getCharacterStream()));
        c.close();
        assertFalse(tmp.exists()); //temp file should be deleted
        //Test the CSOB (small object) 10K
        c = (Lobs.ReadonlyClob) Lobs.newClob(new StringReader(csob));
        assertNull(c.tmpFile);
        assertEquals(csob.length(), c.length());
        assertNull(c.tmpFile);
        assertEquals(csob, IOUtils.toString(c.getCharacterStream()));
        c.close();
        //Test the CLOB with lengtj
        c = (Lobs.ReadonlyClob) Lobs.newClob(new StringReader(clob), clob.length());
        assertNull(c.tmpFile);
        assertEquals(clob.length(), c.length());
        assertNull(c.tmpFile); //tmp file should not created
        c.close();
    }

    /**
     * Test for bug #4903 Java Null Pointer Exception
     * @throws IOException if IO error occurs
     */
    public void testEmptyClob() throws IOException {
        Lobs.ReadonlyClob c = (Lobs.ReadonlyClob) Lobs.newClob(new StringReader(""));
        Reader r = c.getCharacterStream();
        assertEquals("", IOUtils.toString(r));
        assertEquals(0, c.length());
        assertNull(c.tmpFile);
    }

    public void testEmptyBlob() throws IOException {
        Lobs.ReadonlyBlob b = (Lobs.ReadonlyBlob) Lobs.newBlob(new ByteArrayInputStream(new byte[0]));
        InputStream is = b.getBinaryStream();
        assertEquals(0, IOUtils.toByteArray(is).length);
        assertEquals(0, b.length());
    }

}