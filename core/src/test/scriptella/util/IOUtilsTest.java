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
package scriptella.util;

import scriptella.AbstractTestCase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * Tests for {@link scriptella.util.IOUtils}.
 */
public class IOUtilsTest extends AbstractTestCase {
    public void testToUrl() throws MalformedURLException {
        URL url = IOUtils.toUrl(new File("tst 2"));
        assertTrue(url.toString().startsWith("file:/"));
        assertTrue(url.toString().endsWith("tst%202"));
    }

    public void testToByteArray() throws IOException {
        byte[] expected = new byte[]{1, 2, 3, 4};
        assertTrue(Arrays.equals(expected, IOUtils.toByteArray(new ByteArrayInputStream(expected))));
    }

    public void testToCharArray() throws IOException {
        String expected = "test1234\u0000";
        assertEquals(expected, IOUtils.toString(new StringReader(expected)));
    }

    public void testResolve() throws MalformedURLException {
        URL base = new URL("file:/c:/docs/etl.xml");
        assertEquals(new URL("file:/d:"), IOUtils.resolve(base, "d:"));
        assertEquals(new URL("file:/d:/test.txt"), IOUtils.resolve(base, "d:/test.txt"));
        try {
            String malformed = "d://test.txt";
            IOUtils.resolve(base, malformed);
            fail("Malformed url " + malformed + " must be rejected");
        } catch (MalformedURLException e) {
            //OK
        }
    }

    public void testFileUrlOutput() throws IOException {
        Path directory = Files.createTempDirectory("scriptella output ");
        try {
            for (String name : new String[]{"space name", "percent%name", "hash#name", "plus+name", "caf\u00e9"}) {
                Path file = directory.resolve(name);
                try {
                    try (OutputStream out = IOUtils.getOutputStream(file.toUri().toURL())) {
                        out.write(42);
                    }
                    assertTrue("Expected output at " + file, Files.exists(file));
                    assertEquals(42, Files.readAllBytes(file)[0]);
                } finally {
                    Files.deleteIfExists(file);
                }
            }
            Path raw = directory.resolve("legacy space");
            try {
                try (OutputStream out = IOUtils.getOutputStream(new URL("file:" + raw))) {
                    out.write(43);
                }
                assertEquals(43, Files.readAllBytes(raw)[0]);
            } finally {
                Files.deleteIfExists(raw);
            }
        } finally {
            Files.delete(directory);
        }
    }

    public void testRelativeFileUrlOutput() throws IOException, URISyntaxException {
        Path directory = Files.createTempDirectory(Paths.get("."), "scriptella-relative-");
        Path file = directory.resolve("relative +%.txt");
        try {
            String path = file.toString().replace(File.separatorChar, '/');
            URL url = new URL("file:" + new URI(null, null, path, null).toASCIIString());
            assertTrue(url.toURI().isOpaque());
            assertFileOutput(url, file);
        } finally {
            Files.deleteIfExists(file);
            Files.delete(directory);
        }
    }

    public void testFileUrlWithAuthorityOutput() throws IOException {
        Path directory = Files.createTempDirectory("scriptella authority ");
        Path file = directory.resolve("authority +%.txt");
        try {
            // Historically the authority was ignored, even for hosts other than localhost.
            for (String host : new String[]{"localhost", "legacy-host"}) {
                assertFileOutput(new URL("file://" + host + file.toUri().getRawPath()), file);
            }
        } finally {
            Files.deleteIfExists(file);
            Files.delete(directory);
        }
    }

    public void testWindowsOpaqueFileUrlOutput() throws IOException, URISyntaxException {
        if (File.separatorChar != '\\') {
            return; // Requires a Windows filesystem to exercise drive-letter resolution.
        }
        Path directory = Files.createTempDirectory("scriptella-drive-");
        Path file = directory.resolve("drive +%.txt");
        try {
            String path = file.toUri().getRawPath();
            URL url = new URL("file:" + path.substring(1)); // file:C:/... rather than file:/C:/...
            assertTrue(url.toURI().isOpaque());
            assertFileOutput(url, file);
        } finally {
            Files.deleteIfExists(file);
            Files.delete(directory);
        }
    }

    public void testFileUrlFragmentsIgnored() throws IOException, URISyntaxException {
        Path directory = Files.createTempDirectory(Paths.get("."), "scriptella-fragment-");
        Path file = directory.resolve("fragment #+%.txt");
        try {
            for (URL url : fileUrlForms(file)) {
                for (String fragment : new String[]{"#anything", "#ignored%escape"}) {
                    assertFileOutput(new URL(url.toExternalForm() + fragment), file);
                }
            }
        } finally {
            Files.deleteIfExists(file);
            Files.delete(directory);
        }
    }

    public void testFileUrlQueryTextIsPartOfFilename() throws IOException, URISyntaxException {
        if (File.separatorChar == '\\') {
            return; // Windows filenames cannot contain '?'.
        }
        Path directory = Files.createTempDirectory(Paths.get("."), "scriptella-query-");
        Path base = directory.resolve("query +%.txt");
        Path file = directory.resolve("query +%.txt?value=1?more");
        try {
            for (URL url : fileUrlForms(base)) {
                assertFileOutput(new URL(url.toExternalForm() + "?value=1?more#ignored"), file);
                assertFalse("Query text must not be silently discarded", Files.exists(base));
            }
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(base);
            Files.delete(directory);
        }
    }

    private URL[] fileUrlForms(Path file) throws IOException, URISyntaxException {
        String relative = file.toString().replace(File.separatorChar, '/');
        return new URL[]{file.toUri().toURL(),
                new URL("file://localhost" + file.toUri().getRawPath()),
                new URL("file:" + new URI(null, null, relative, null).toASCIIString())};
    }

    private void assertFileOutput(URL url, Path file) throws IOException {
        try (OutputStream out = IOUtils.getOutputStream(url)) {
            out.write(44);
        }
        assertEquals(44, Files.readAllBytes(file)[0]);
    }

    public void testMalformedFileUrlOutput() throws IOException {
        try {
            IOUtils.getOutputStream(new URL("file:/invalid%escape"));
            fail("Malformed URI escape should be rejected");
        } catch (IOException expected) {
            assertNotNull(expected.getCause());
        }
    }

}
