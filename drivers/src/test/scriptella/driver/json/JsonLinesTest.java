/*
 * Copyright 2026 The Scriptella Project Team.
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
package scriptella.driver.json;

import junit.framework.TestCase;
import scriptella.configuration.StringResource;
import scriptella.core.EtlCancelledException;
import scriptella.spi.MockConnectionParameters;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JsonLinesTest extends TestCase {
    private static class Input extends JsonConnection {
        final Reader source;
        boolean closed;

        Input(String text) {
            this(new StringReader(text));
        }

        Input(Reader source) {
            super(new MockConnectionParameters(Collections.singletonMap("format", "jsonl"), null));
            this.source = source;
        }

        protected Reader newInputReader() {
            return new Reader() {
                public int read(char[] chars, int offset, int length) throws IOException {
                    return source.read(chars, offset, length);
                }
                public void close() throws IOException {
                    closed = true;
                    source.close();
                }
            };
        }
    }

    private List<Object> read(Input input) {
        List<Object> rows = new ArrayList<Object>();
        input.executeQuery(new StringResource(""), name -> "parent", row -> {
            assertEquals("parent", row.getParameter("other"));
            rows.add(row.getParameter("row"));
        });
        assertTrue(input.closed);
        assertEquals(1L, input.getExecutedStatementsCount());
        return rows;
    }

    public void testEquivalentValuesAndLineEndings() {
        String values = "{\"name\":\"雪\\n\",\"n\":12.50}\n[1,null]\r\ntrue\nnull\n"
                + "9223372036854775808\n\"text\"";
        List<Object> actual = read(new Input(values));
        List<Object> expected = new ArrayList<Object>();
        JsonConnection standard = new JsonConnection(new MockConnectionParameters()) {
            protected Reader newInputReader() {
                return new StringReader("[" + values.replace("\r", "").replace("\n", ",") + "]");
            }
        };
        standard.executeQuery(new StringResource(""), name -> null,
                row -> expected.add(row.getParameter("row")));
        assertEquals(expected, actual);
        assertTrue(read(new Input("")).isEmpty());
        assertEquals(Arrays.asList(1L), read(new Input("1\n")));
        assertEquals(Arrays.asList(1L), read(new Input("1\r\n")));
    }

    public void testMalformedLinesFailBeforeCallback() {
        for (String bad : new String[]{"", " ", "\uFEFF{}", "{} {}", "{", "[1,]",
                "{\"a\":1,\"a\":2}", "/*comment*/{}", "NaN", "\uFEFF1", "{}\r1"}) {
            Input input = new Input("1\n" + bad + "\n");
            List<Object> rows = new ArrayList<Object>();
            try {
                input.executeQuery(new StringResource(""), name -> null,
                        row -> rows.add(row.getParameter("row")));
                fail("Accepted malformed line");
            } catch (JsonProviderException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("line 2, record 2"));
                assertEquals(Arrays.asList(1L), rows);
                assertTrue(input.closed);
                assertEquals(0L, input.getExecutedStatementsCount());
            }
        }
        Input input = new Input("{}");
        try {
            input.executeQuery(new StringResource("items"), name -> null, row -> fail());
            fail();
        } catch (JsonProviderException expected) {
            assertTrue(expected.getMessage().contains("empty selection"));
        }
    }

    public void testCallbackCancellationAndCleanupFailures() {
        Input input = new Input(new StringReader("1\n2\n") {
            public void close() {
                throw new IllegalStateException("cleanup");
            }
        });
        RuntimeException failure = new RuntimeException("callback");
        try {
            input.executeQuery(new StringResource(""), name -> null, row -> { throw failure; });
            fail();
        } catch (RuntimeException expected) {
            assertSame(failure, expected);
            assertEquals("cleanup", expected.getSuppressed()[0].getMessage());
            assertTrue(input.closed);
        }
        Input cancelled = new Input("1\n2\n");
        try {
            cancelled.executeQuery(new StringResource(""), name -> null,
                    row -> Thread.currentThread().interrupt());
            fail();
        } catch (EtlCancelledException expected) {
            assertTrue(cancelled.closed);
        } finally {
            Thread.interrupted();
        }
    }

    public void testLazyOutputFramingAndFailedExport() throws Exception {
        Path file = Files.createTempFile("json-lines", ".jsonl");
        try {
            MockConnectionParameters parameters = new MockConnectionParameters(
                    Collections.singletonMap("format", "jsonl"), file.toUri().toString());
            Files.writeString(file, "untouched");
            JsonConnection output = new JsonConnection(parameters);
            output.commit();
            output.close();
            assertEquals("untouched", Files.readString(file));
            Files.delete(file);
            output = new JsonConnection(parameters);
            output.commit();
            output.close();
            assertFalse(Files.exists(file));
            output = new JsonConnection(parameters);
            output.executeScript(new StringResource("{\"a\":?{value}}"), name -> "雪\n");
            output.executeScript(new StringResource("{\"b\":[true,null]}"), name -> null);
            output.commit();
            output.close();
            assertEquals("{\"a\":\"雪\\n\"}\n{\"b\":[true,null]}\n", Files.readString(file));
            assertEquals(2, read(new Input(Files.readString(file))).size());
            output = new JsonConnection(parameters);
            output.executeScript(new StringResource("{}"), name -> null);
            try {
                output.executeScript(new StringResource("{bad}"), name -> null);
                fail();
            } catch (JsonProviderException expected) {
                // A complete line is still an incomplete export after a handled error.
            }
            try {
                output.commit();
                fail();
            } catch (JsonProviderException expected) {
                assertTrue(expected.getMessage().contains("incomplete"));
            }
            output.close();
            assertEquals("{}\n", Files.readString(file));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    public void testNewlineWriteAndFlushFailures() {
        for (boolean flushFailure : new boolean[]{false, true}) {
            StringWriter bytes = new StringWriter();
            boolean[] closed = {false};
            JsonConnection output = new JsonConnection(new MockConnectionParameters(
                    Collections.singletonMap("format", "jsonl"), null)) {
                protected Writer newOutputWriter() {
                    return new Writer() {
                        public void write(char[] chars, int offset, int length) throws IOException {
                            if (!flushFailure && chars[offset] == '\n') {
                                throw new IOException("newline");
                            }
                            bytes.write(chars, offset, length);
                        }
                        public void flush() throws IOException {
                            if (flushFailure) {
                                throw new IOException("flush");
                            }
                        }
                        public void close() {
                            closed[0] = true;
                        }
                    };
                }
            };
            try {
                output.executeScript(new StringResource("{}"), name -> null);
                output.commit();
                fail();
            } catch (JsonProviderException expected) {
                assertEquals(flushFailure ? "flush" : "newline", expected.getCause().getMessage());
            } finally {
                output.close();
            }
            assertTrue(closed[0]);
            assertEquals(flushFailure ? "{}\n" : "{}", bytes.toString());
        }
    }
    public void testStreamingBeforeEndOfInput() {
        int[] emitted = {0};
        Input input = new Input(new Reader() {
            int position;
            public int read(char[] buffer, int offset, int length) {
                if (position == 200000) {
                    return -1;
                }
                if (position > 10000) {
                    assertTrue(emitted[0] > 0);
                }
                buffer[offset] = position++ % 2 == 0 ? '0' : '\n';
                return 1;
            }
            public void close() { }
        });
        input.executeQuery(new StringResource(""), name -> null, row -> emitted[0]++);
        assertEquals(100000, emitted[0]);
        assertTrue(input.closed);
    }

    public void testEtlTransformationAndStrictDecoding() throws Exception {
        Path directory = Files.createTempDirectory("jsonl-etl");
        try {
            Path source = directory.resolve("input.jsonl");
            Files.writeString(source, "{\"items\":[1,null],\"n\":12.50}\r\nnull");
            String xml = "<!DOCTYPE etl SYSTEM \"http://scriptella.org/dtd/etl.dtd\"><etl>"
                    + "<connection id=\"in\" driver=\"json\" url=\"input.jsonl\">format=jsonl</connection>"
                    + "<connection id=\"out\" driver=\"json\" url=\"output.jsonl\">format=jsonl</connection>"
                    + "<query connection-id=\"in\"><script connection-id=\"out\">"
                    + "{\"copy\":?{row}}</script></query></etl>";
            Path etl = directory.resolve("etl.xml");
            Files.writeString(etl, xml);
            scriptella.execution.EtlExecutor.newExecutor(etl.toFile()).execute();
            assertEquals("{\"copy\":{\"items\":[1,null],\"n\":12.50}}\n{\"copy\":null}\n",
                    Files.readString(directory.resolve("output.jsonl")));
            for (byte[] bytes : new byte[][]{
                    "\uFEFF{}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    new byte[]{'"', (byte) 0xc3, '"', '\n'}}) {
                Files.write(source, bytes);
                JsonConnection input = new JsonConnection(new MockConnectionParameters(
                        Collections.singletonMap("format", "jsonl"), source.toUri().toString()));
                try {
                    input.executeQuery(new StringResource(""), name -> null, row -> fail());
                    fail();
                } catch (JsonProviderException expected) {
                    assertTrue(expected.getMessage().contains("line 1, record 1"));
                } finally {
                    input.close();
                }
            }
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
                for (Path path : (Iterable<Path>) paths.sorted(java.util.Comparator.reverseOrder())::iterator) {
                    Files.delete(path);
                }
            }
        }
    }

    public void testFormatSelectionForReadingAndWriting() {
        String[][] cases = {
                {null, null, "json"},
                {null, "https://example.test/data", "json"},
                {null, "https://example.test/data.json", "json"},
                {null, "https://example.test/data.csv", "json"},
                {null, "https://example.test/data.jsonl", "jsonl"},
                {null, "https://example.test/data.NdJsOn", "jsonl"},
                {null, "https://example.test/data.JSONL?format=json#data.json", "jsonl"},
                {null, "https://example.test/data.ndjson?file=data.json#x", "jsonl"},
                {null, "https://example.test/data.json?file=data.jsonl#data.ndjson", "json"},
                {null, "https://example.test/data.jsonl/child", "json"},
                {null, "file:/tmp/data.NDJSON", "jsonl"},
                {"json", "https://example.test/data.jsonl", "json"},
                {"json", "https://example.test/data.ndjson", "json"},
                {"jsonl", "https://example.test/data.json", "jsonl"},
                {"jsonl", null, "jsonl"},
                {"json", null, "json"}
        };
        for (String[] item : cases) {
            boolean lines = "jsonl".equals(item[2]);
            MockConnectionParameters parameters = new MockConnectionParameters(
                    item[0] == null ? Collections.emptyMap() : Collections.singletonMap("format", item[0]),
                    item[1]);
            JsonConnection input = new JsonConnection(parameters) {
                protected Reader newInputReader() {
                    return new StringReader(lines ? "1\n2\n" : "[1,2]");
                }
            };
            List<Object> rows = new ArrayList<Object>();
            input.executeQuery(new StringResource(""), name -> null,
                    row -> rows.add(row.getParameter("row")));
            input.close();
            assertEquals(Arrays.asList(1L, 2L), rows);
            StringWriter bytes = new StringWriter();
            JsonConnection output = new JsonConnection(parameters) {
                protected Writer newOutputWriter() {
                    return bytes;
                }
            };
            output.executeScript(new StringResource("{}"), name -> null);
            output.executeScript(new StringResource("{}"), name -> null);
            output.commit();
            output.close();
            assertEquals(Arrays.toString(item), lines ? "{}\n{}\n" : "[{},{}]", bytes.toString());
        }
    }

    public void testMalformedLineSanitizesSuppressedInputCloseFailure() {
        String source = "https://alice:private%2Dpassword@example.test/input.jsonl"
                + "?token=private%2Dtoken&api_key=private-key";
        IOException cleanup = new IOException("Cannot close " + source,
                new IllegalStateException("password=private-password token=private-token"));
        cleanup.addSuppressed(new IOException("api key private-key for alice"));
        boolean[] closed = {false};
        JsonConnection input = new JsonConnection(new MockConnectionParameters(Collections.emptyMap(), source)) {
            protected Reader newInputReader() {
                return new Reader() {
                    final StringReader content = new StringReader("1\n{broken}\n");
                    public int read(char[] chars, int offset, int length) throws IOException {
                        return content.read(chars, offset, length);
                    }
                    public void close() throws IOException {
                        closed[0] = true;
                        throw cleanup;
                    }
                };
            }
        };
        List<Object> rows = new ArrayList<Object>();
        try {
            input.executeQuery(new StringResource(""), name -> null,
                    row -> rows.add(row.getParameter("row")));
            fail();
        } catch (JsonProviderException expected) {
            assertTrue(closed[0]);
            assertEquals(Arrays.asList(1L), rows);
            assertTrue(expected.getMessage().contains("line 2, record 2"));
            Throwable original = expected;
            assertNotNull(original.getCause());
            assertEquals(1, original.getSuppressed().length);
            assertTrue(original.getSuppressed()[0] instanceof IOException);
            assertNotSame(cleanup, original.getSuppressed()[0]);
            java.io.StringWriter trace = new java.io.StringWriter();
            expected.printStackTrace(new java.io.PrintWriter(trace));
            for (String secret : new String[]{source, "alice", "private-password",
                    "private%2Dpassword", "private-token", "private%2Dtoken", "private-key"}) {
                assertFalse("Secret leaked in stack trace", trace.toString().contains(secret));
            }
            assertTrue(trace.toString().contains("column"));
            assertTrue(trace.toString().contains("Suppressed:"));
            assertTrue(trace.toString().contains("Cannot close"));
            assertTrue(cleanup.getMessage().contains(source));
            assertEquals(0L, input.getExecutedStatementsCount());
        } finally {
            input.close();
        }
    }

    public void testJsonProviderCallbackFailurePropagatesUnchanged() {
        for (boolean cleanupFails : new boolean[]{false, true}) {
            String source = "https://reader:reader-password@example.test/input.jsonl?token=reader-token";
            IOException cleanup = new IOException("Cannot close " + source);
            boolean[] closed = {false};
            JsonConnection input = new JsonConnection(
                    new MockConnectionParameters(Collections.emptyMap(), source)) {
                protected Reader newInputReader() {
                    return new Reader() {
                        final StringReader content = new StringReader("1\n2\n");
                        public int read(char[] chars, int offset, int length) throws IOException {
                            return content.read(chars, offset, length);
                        }
                        public void close() throws IOException {
                            closed[0] = true;
                            if (cleanupFails) {
                                throw cleanup;
                            }
                        }
                    };
                }
            };
            IllegalArgumentException cause = new IllegalArgumentException("writer mapping failure");
            JsonProviderException failure = new JsonProviderException("downstream JSON writer", cause);
            IllegalStateException existing = new IllegalStateException("existing writer diagnostic");
            failure.addSuppressed(existing);
            try {
                input.executeQuery(new StringResource(""), name -> null, row -> { throw failure; });
                fail();
            } catch (JsonProviderException expected) {
                assertSame(failure, expected);
                assertSame(cause, expected.getCause());
                assertSame(existing, expected.getSuppressed()[0]);
                assertEquals(cleanupFails ? 2 : 1, expected.getSuppressed().length);
                assertTrue(closed[0]);
                if (cleanupFails) {
                    Throwable sanitized = expected.getSuppressed()[1];
                    assertTrue(sanitized instanceof IOException);
                    assertNotSame(cleanup, sanitized);
                    StringWriter trace = new StringWriter();
                    expected.printStackTrace(new java.io.PrintWriter(trace));
                    for (String secret : new String[]{source, "reader-password", "reader-token"}) {
                        assertFalse(trace.toString().contains(secret));
                    }
                    assertTrue(trace.toString().contains("Cannot close"));
                }
                assertEquals(0L, input.getExecutedStatementsCount());
            } finally {
                input.close();
            }
        }
    }

}
