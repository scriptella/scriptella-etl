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
import scriptella.core.EtlCancelledException;
import scriptella.spi.MockConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.configuration.StringResource;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JsonConnectionTest extends TestCase {
    private static final ParametersCallback PARENT = name -> "parent:" + name;

    private static class Input extends JsonConnection {
        boolean inputClosed;
        final Reader reader;

        Input(String json) {
            this(new StringReader(json));
        }

        Input(Reader reader) {
            super(new MockConnectionParameters());
            this.reader = reader;
        }

        @Override
        protected Reader newInputReader() {
            return new Reader() {
                public int read(char[] buffer, int offset, int length) throws IOException {
                    return reader.read(buffer, offset, length);
                }

                public void close() throws IOException {
                    inputClosed = true;
                    reader.close();
                }
            };
        }
    }

    private List<Object> read(String json, String path) {
        Input input = new Input(json);
        List<Object> rows = new ArrayList<Object>();
        input.executeQuery(new StringResource(path), PARENT, parameters -> {
            assertEquals("parent:other", parameters.getParameter("other"));
            rows.add(parameters.getParameter("row"));
        });
        assertTrue(input.inputClosed);
        assertEquals(1L, input.getExecutedStatementsCount());
        input.close();
        return rows;
    }

    private void reject(String json, String path) {
        Input input = new Input(json);
        try {
            input.executeQuery(new StringResource(path), PARENT, parameters -> { });
            fail("Expected JSON failure");
        } catch (JsonProviderException expected) {
            assertTrue(input.inputClosed);
            assertNotNull(expected.getMessage());
        }
    }

    public void testRootAndTypedValues() {
        List<Object> rows = read("[null,true,false,1,-9223372036854775808,9223372036854775808,12.50,1e3,\"雪\\n\\\"\",[1],{}]", "  ");
        assertEquals(11, rows.size());
        assertNull(rows.get(0));
        assertEquals(Boolean.TRUE, rows.get(1));
        assertEquals(Boolean.FALSE, rows.get(2));
        assertEquals(1L, rows.get(3));
        assertEquals(Long.MIN_VALUE, rows.get(4));
        assertEquals(new BigInteger("9223372036854775808"), rows.get(5));
        assertEquals(new BigDecimal("12.50"), rows.get(6));
        assertEquals(new BigDecimal("1e3"), rows.get(7));
        assertEquals("雪\n\"", rows.get(8));
        assertEquals(Arrays.asList(1L), rows.get(9));
        assertEquals(Collections.emptyMap(), rows.get(10));
        Map<?, ?> object = (Map<?, ?>) read("{\"b\":null,\"a\":2}", "").get(0);
        assertEquals(Arrays.asList("b", "a"), new ArrayList<Object>(object.keySet()));
        assertTrue(object.containsKey("b"));
        assertNull(object.get("absent"));
        assertTrue(read("[]", "").isEmpty());
    }

    public void testSelectionPaths() {
        assertEquals(Arrays.asList(1L, null), read("{\"skip\":{},\"response\":{\"customers\":[1,null]},\"tail\":true}", "response.customers"));
        assertEquals(Arrays.asList(2L), read("{\"batches\":[{\"rows\":[2]},null]}", "batches[0].rows"));
        assertEquals(Arrays.asList(3L), read("{\"response.data\":{\"雪\":[3]}}", "['response.data'][\"\\u96ea\"]"));
        assertEquals(Arrays.asList(4L), read("[{\"\":[4]}]", "[0]['']"));
        assertEquals(Arrays.asList(5L), read("{\"a'b\\\\c\":[5]}", "['a\\'b\\\\c']"));
        assertEquals(Arrays.asList(6L), read("{\"a\\\"b\":[6]}", "['a\"b']"));
        assertEquals(Arrays.asList(7L), read("{\"0\":[7]}", "[\"0\"]"));
        assertTrue(read("{\"x\":[]}", "x").isEmpty());
    }

    public void testInvalidPaths() {
        for (String path : new String[]{"$", "a..b", "a.", "a b", "a[ 0]", "a[-1]", "a[01]", "a[2147483648]", "a[*]", "a[]", "a[0:1]", "a[", "a['x'", "a['\\q']", "a[\"\\'\"]", "a['x']z", ".x", "[0].['x']"}) {
            try {
                JsonPath.parse(path);
                fail("Accepted invalid path: " + path);
            } catch (JsonProviderException expected) {
                assertTrue(expected.getMessage().contains("column"));
            }
        }
    }

    public void testInvalidSelectionAndFraming() {
        for (String document : new String[]{"", " \n", "null", "1", "true", "\"x\"", "[1,]", "[01]", "[NaN]", "[1] []", "{} true", "/*x*/[]", "[", "[{\"x\":1", "[\"\\q\"]"}) {
            reject(document, "");
        }
        for (String document : new String[]{"{}", "{\"x\":null}", "{\"x\":1}", "{\"x\":{}}", "[]"}) {
            reject(document, "x");
        }
        reject("{\"x\":[0]}", "x[1].rows");
        reject("{\"x\":{\"0\":[]}}", "x[0]");
        reject("{\"x\":[]}", "x['0']");
        reject("{\"x\":[],\"tail\":[1,]}", "x");
        reject("{\"x\":[],\"tail\":\"\\q\"}", "x");
    }

    public void testDuplicatePolicy() {
        reject("[{\"x\":null,\"x\":1}]", "");
        reject("[{\"nested\":{\"a\":1,\"\\u0061\":2}}]", "");
        reject("{\"x\":[],\"x\":[]}", "x");
        reject("{\"x\":{\"y\":[]},\"x\":{\"y\":[]}}", "x.y");
        assertEquals(Arrays.asList(1L), read("{\"unused\":0,\"unused\":1,\"skip\":{\"a\":1,\"a\":2},\"x\":[1]}", "x"));
    }

    public void testCallbackFailureAndCancellationCloseInput() {
        Input failing = new Input("[1,2]");
        RuntimeException failure = new RuntimeException("downstream");
        try {
            failing.executeQuery(new StringResource(""), PARENT, row -> { throw failure; });
            fail();
        } catch (RuntimeException expected) {
            assertSame(failure, expected);
            assertTrue(failing.inputClosed);
        }
        Input cancelled = new Input("[1,2]");
        try {
            cancelled.executeQuery(new StringResource(""), PARENT, row -> Thread.currentThread().interrupt());
            fail();
        } catch (EtlCancelledException expected) {
            assertTrue(cancelled.inputClosed);
        } finally {
            Thread.interrupted();
        }
        Input skipped = new Input(new Reader() {
            int offset;
            public int read(char[] buffer, int start, int length) {
                String text = "{\"skip\":[0,1,2],\"x\":[]}";
                if (offset == 12) {
                    Thread.currentThread().interrupt();
                }
                if (offset == text.length()) {
                    return -1;
                }
                buffer[start] = text.charAt(offset++);
                return 1;
            }
            public void close() { }
        });
        try {
            skipped.executeQuery(new StringResource("x"), PARENT, row -> fail());
            fail();
        } catch (EtlCancelledException expected) {
            assertTrue(skipped.inputClosed);
        } finally {
            Thread.interrupted();
        }
    }

    public void testStrictDecodingBomAndEncoding() throws Exception {
        Path file = Files.createTempFile("scriptella-json", ".json");
        try {
            Files.write(file, "\uFEFF[\"雪\"]".getBytes("UTF-8"));
            JsonConnection connection = new JsonConnection(new MockConnectionParameters(Collections.emptyMap(), file.toUri().toString()));
            List<Object> rows = new ArrayList<Object>();
            connection.executeQuery(new StringResource(""), PARENT, row -> rows.add(row.getParameter("row")));
            assertEquals(Arrays.asList("雪"), rows);
            Files.write(file, new byte[]{'[', '"', (byte) 0xc3, '"', ']'});
            try {
                connection.executeQuery(new StringResource(""), PARENT, row -> fail());
                fail();
            } catch (JsonProviderException expected) {
                assertTrue(expected.getCause() instanceof IOException);
            }
            Files.write(file, "[\"é\"]".getBytes("ISO-8859-1"));
            connection = new JsonConnection(new MockConnectionParameters(Collections.singletonMap("encoding", "ISO-8859-1"), file.toUri().toString()));
            rows.clear();
            connection.executeQuery(new StringResource(""), PARENT, row -> rows.add(row.getParameter("row")));
            assertEquals(Arrays.asList("é"), rows);
        } finally {
            Files.delete(file);
        }
    }

    public void testConfigurationAndLifecycle() {
        try {
            new JsonConnection(new MockConnectionParameters(Collections.singletonMap("format", "jsonl"), null));
            fail();
        } catch (JsonProviderException expected) {
            assertTrue(expected.getMessage().contains("format"));
        }
        Input input = new Input("[]");
        input.executeQuery(new StringResource(""), PARENT, row -> fail());
        try {
            input.executeScript(new StringResource("{}"), PARENT);
            fail();
        } catch (JsonProviderException expected) {
            assertTrue(expected.getMessage().contains("same JSON connection"));
        }
        input.close();
        try {
            input.executeQuery(new StringResource(""), PARENT, row -> fail());
            fail();
        } catch (JsonProviderException expected) {
            assertTrue(expected.getMessage().contains("closed"));
        }
    }

    public void testInputErrorDoesNotExposeUrlCredentials() {
        String source = "https://alice:private%2Dpassword@example.test/customers.json?token=private%2Dtoken&api_key=private-key";
        IOException failure = new IOException("Cannot open " + source,
                new IllegalStateException("password=private-password token=private-token"));
        IOException suppressed = new IOException("close " + source);
        suppressed.addSuppressed(new IOException("api key private-key for alice"));
        failure.addSuppressed(suppressed);
        // A cycle must not retain a raw exception or prevent diagnostic copying.
        suppressed.initCause(failure);
        JsonConnection connection = new JsonConnection(
                new MockConnectionParameters(Collections.emptyMap(), source)) {
            @Override
            protected Reader newInputReader() throws IOException {
                throw failure;
            }
        };
        try {
            connection.executeQuery(new StringResource(""), PARENT, row -> fail());
            fail();
        } catch (JsonProviderException expected) {
            assertEquals("Cannot read JSON input URL scheme https:", expected.getMessage());
            assertNotSame(failure, expected.getCause());
            assertTrue(expected.getCause() instanceof IOException);
            assertEquals(Arrays.asList(failure.getStackTrace()),
                    Arrays.asList(expected.getCause().getStackTrace()));
            StringWriter output = new StringWriter();
            expected.printStackTrace(new PrintWriter(output));
            String trace = output.toString();
            for (String secret : new String[]{source, "alice", "private-password", "private%2Dpassword",
                    "private-token", "private%2Dtoken", "private-key"}) {
                assertFalse("Secret found in reported stack trace", trace.contains(secret));
            }
            assertTrue(trace.contains("Cannot open"));
            assertTrue(trace.contains("IllegalStateException"));
            assertTrue(trace.contains("Suppressed:"));
            assertTrue(trace.contains("close"));
            assertTrue(trace.contains("api key"));
            assertTrue(failure.getMessage().contains(source));
        }
    }

    public void testIoFailuresAndSuppressedCleanup() {
        Input input = new Input(new Reader() {
            public int read(char[] buffer, int offset, int length) throws IOException {
                throw new IOException("read failure");
            }
            public void close() throws IOException {
                throw new IOException("close failure");
            }
        });
        try {
            input.executeQuery(new StringResource(""), PARENT, row -> fail());
            fail();
        } catch (JsonProviderException expected) {
            assertTrue(input.inputClosed);
            assertEquals("read failure", expected.getCause().getMessage());
            assertEquals("close failure", expected.getCause().getSuppressed()[0].getMessage());
            assertEquals(0L, input.getExecutedStatementsCount());
        }
        Input callbackFailure = new Input(new StringReader("[1]") {
            @Override
            public void close() {
                throw new IllegalStateException("cleanup failure");
            }
        });
        RuntimeException failure = new RuntimeException("callback failure");
        try {
            callbackFailure.executeQuery(new StringResource(""), PARENT, row -> { throw failure; });
            fail();
        } catch (RuntimeException expected) {
            assertSame(failure, expected);
            assertTrue(callbackFailure.inputClosed);
            assertEquals("cleanup failure", expected.getSuppressed()[0].getMessage());
        }
    }

    public void testParserConstraints() {
        StringBuilder number = new StringBuilder("[");
        for (int i = 0; i < 1100; i++) {
            number.append('1');
        }
        reject(number.append(']').toString(), "");
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 1002; i++) {
            deep.append('[');
        }
        for (int i = 0; i < 1002; i++) {
            deep.append(']');
        }
        reject(deep.toString(), "");
        // Constraints apply even to an unrelated subtree.
        reject("{\"x\":[],\"skip\":" + deep + "}", "x");
    }

    public void testStreamingBeforeEndOfInput() {
        final int[] emitted = {0};
        // Generate input on demand; never store the document in the test or reader.
        Reader generated = new Reader() {
            int position;
            public int read(char[] buffer, int offset, int length) {
                if (position > 200000) {
                    return -1;
                }
                if (position > 10000) {
                    assertTrue("Callbacks must start before consuming the document", emitted[0] > 0);
                }
                buffer[offset] = position == 0 ? '[' : position == 200000 ? ']' : position % 2 == 0 ? ',' : '0';
                position++;
                return 1;
            }
            public void close() { }
        };
        Input input = new Input(generated);
        input.executeQuery(new StringResource(""), PARENT, row -> emitted[0]++);
        assertEquals(100000, emitted[0]);
        assertTrue(input.inputClosed);
    }
}
