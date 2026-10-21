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
import scriptella.spi.MockConnectionParameters;
import scriptella.spi.Resource;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonWriterTest extends TestCase {
    private static class Output extends JsonConnection {
        final StringWriter buffer = new StringWriter();
        int opens;
        boolean outputClosed;

        Output() {
            super(new MockConnectionParameters());
        }

        protected Writer newOutputWriter() {
            opens++;
            return new Writer() {
                public void write(char[] chars, int start, int length) {
                    buffer.write(chars, start, length);
                }
                public void flush() { }
                public void close() { outputClosed = true; }
            };
        }
    }

    public void testTypesOrderEscapingAndRepeatedInvocations() {
        Output output = new Output();
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("name", "雪\n\r\t\u0000\"},\"injected\":true");
        data.put("decimal", new BigDecimal("12345678901234567890.00100"));
        data.put("integer", new BigInteger("9223372036854775808"));
        data.put("list", Arrays.asList(true, null, Collections.singletonMap("a", 1)));
        StringResource script = new StringResource("{\"data\":?{data},\"fixed\":[1,null,?{flag}],\"literal\":\"?{x} ${x}\"}");
        output.executeScript(script, name -> name.equals("data") ? data : false);
        output.executeScript(script, name -> name.equals("data") ? data : true);
        output.commit();
        output.commit();
        assertTrue(output.outputClosed);
        assertEquals(1, output.opens);
        assertEquals(2L, output.getExecutedStatementsCount());
        String first = "{\"data\":";
        assertTrue(output.buffer.toString().startsWith("[" + first));
        assertTrue(output.buffer.toString().contains("12345678901234567890.00100"));
        assertTrue(output.buffer.toString().contains("9223372036854775808"));
        assertTrue(output.buffer.toString().contains("\\\"injected\\\":true"));
        assertTrue(output.buffer.toString().contains("\"literal\":\"?{x} ${x}\""));
        assertEquals(2, rows(output.buffer.toString()).size());
    }

    private List<Object> rows(String json) {
        JsonConnection input = new JsonConnection(new MockConnectionParameters()) {
            protected java.io.Reader newInputReader() { return new StringReader(json); }
        };
        List<Object> rows = new ArrayList<Object>();
        input.executeQuery(new StringResource(""), name -> null, row -> rows.add(row.getParameter("row")));
        input.close();
        return rows;
    }

    public void testExpressionScanner() {
        Output output = new Output();
        output.executeScript(new StringResource("{\"s\":?{'a}b'},\"m\":?{{'key':'value'}},"
                + "\"a\":[?{(1 + 2)},?{row['x}y']}],\"comment\":?{1 /* } */ + 2},"
                + "\"line\":?{1 // }\n + 2},\"quote\":?{'a\\'b'}}"),
                name -> Collections.singletonMap("x}y", "yes"));
        output.commit();
        Map<?, ?> row = (Map<?, ?>) rows(output.buffer.toString()).get(0);
        assertEquals("a}b", row.get("s"));
        assertEquals(Collections.singletonMap("key", "value"), row.get("m"));
        assertEquals(Arrays.asList(3L, "yes"), row.get("a"));
        assertEquals(3L, row.get("comment"));
        assertEquals(3L, row.get("line"));
        assertEquals("a'b", row.get("quote"));
    }

    public void testInvalidTemplatesNeverOpenOutput() {
        for (String template : new String[]{"", "[]", "null", "{} {}", "{\"a\":1,}", "{/*x*/}",
                "{\"a\":1,\"\\u0061\":2}", "{?{key}:1}", "{\"a\":?{}}", "{\"a\":?{x}",
                "{\"a\":?{'x}}", "{\"a\":?{(x]}}", "{\"a\":?{1 +}}", "{\"a\":1?{x}}",
                "{\"a\":?{x}2}", "{\"a\":NaN}", "{\"a\":\"\\q\"}"}) {
            Output output = new Output();
            try {
                output.executeScript(new StringResource(template), name -> 1);
                fail("Accepted: " + template);
            } catch (JsonProviderException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("line"));
                assertTrue(expected.getMessage().contains("column"));
                assertTrue(expected.getMessage().contains("output path"));
                assertEquals(0, output.opens);
                assertEquals(0L, output.getExecutedStatementsCount());
            }
        }
    }

    public void testUnsupportedValuesAndCycles() {
        List<Object> cycle = new ArrayList<Object>();
        cycle.add(cycle);
        Map<Object, Object> duplicate = new IdentityHashMap<Object, Object>();
        duplicate.put(new String("key"), 1);
        duplicate.put(new String("key"), 2);
        for (Object value : new Object[]{Double.NaN, Float.POSITIVE_INFINITY, new java.util.Date(),
                java.time.Instant.now(), new Object(), new int[]{1}, Collections.singleton(1),
                new java.util.concurrent.atomic.AtomicInteger(1), Collections.singletonMap(1, 2),
                Collections.singletonMap(null, 2), cycle, duplicate}) {
            Output output = new Output();
            output.executeScript(new StringResource("{\"ok\":1}"), name -> null);
            String before = output.buffer.toString();
            try {
                output.executeScript(new StringResource("{\n\"bad\":?{value}}"), name -> value);
                fail("Accepted type " + value.getClass());
            } catch (JsonProviderException expected) {
                assertTrue(expected.getMessage().contains("line 2"));
                assertTrue(expected.getMessage().contains("$[bad]"));
                assertEquals(before, output.buffer.toString());
                assertEquals(1L, output.getExecutedStatementsCount());
            } finally {
                output.close();
            }
        }
        Output shared = new Output();
        List<Integer> list = Arrays.asList(1, 2);
        shared.executeScript(new StringResource("{\"shared\":?{value}}"), name -> Arrays.asList(list, list));
        shared.commit();
        assertEquals("[{\"shared\":[[1,2],[1,2]]}]", shared.buffer.toString());
    }

    public void testChangingResourceAndCacheReuse() throws Exception {
        Output output = new Output();
        String[] content = {"{\"a\":?{value}}"};
        int[] opens = {0};
        Resource resource = () -> { opens[0]++; return new StringReader(content[0]); };
        output.executeScript(resource, name -> 1);
        java.lang.reflect.Field field = JsonConnection.class.getDeclaredField("template");
        field.setAccessible(true);
        Object cached = field.get(output);
        output.executeScript(resource, name -> 2);
        assertSame(cached, field.get(output));
        content[0] = "{\"b\":?{value}}";
        output.executeScript(resource, name -> 3);
        assertNotSame(cached, field.get(output));
        output.commit();
        assertEquals(3, opens[0]);
        assertEquals("[{\"a\":1},{\"a\":2},{\"b\":3}]", output.buffer.toString());
    }

    public void testLazyFilesReadonlyAndDirection() throws Exception {
        Path file = Files.createTempFile("json-writer", ".json");
        try {
            Files.writeString(file, "untouched");
            JsonConnection connection = new JsonConnection(new MockConnectionParameters(Collections.emptyMap(), file.toUri().toString()));
            connection.commit();
            connection.close();
            assertEquals("untouched", Files.readString(file));
            Files.delete(file);
            connection = new JsonConnection(new MockConnectionParameters(Collections.emptyMap(), file.toUri().toString()));
            connection.commit();
            connection.close();
            assertFalse(Files.exists(file));
            connection = new JsonConnection(new MockConnectionParameters(Collections.singletonMap("readonly", "true"), file.toUri().toString()));
            connection.executeScript(new StringResource("{}"), name -> null);
            connection.commit();
            assertEquals(0L, connection.getExecutedStatementsCount());
            connection.close();
            assertFalse(Files.exists(file));
            Output output = new Output();
            output.executeScript(new StringResource("{}"), name -> null);
            try {
                output.executeQuery(new StringResource(""), name -> null, row -> fail());
                fail();
            } catch (JsonProviderException expected) {
                assertTrue(expected.getMessage().contains("same JSON connection"));
            }
            output.commit();
            try {
                output.executeScript(new StringResource("{}"), name -> null);
                fail();
            } catch (JsonProviderException expected) {
                assertTrue(expected.getMessage().contains("finalized"));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    public void testExpressionFailurePreventsSuccessfulCommit() {
        Output output = new Output();
        output.executeScript(new StringResource("{}"), name -> null);
        try {
            output.executeScript(new StringResource("{\"bad\":?{value}}"), name -> {
                throw new IllegalArgumentException("parameter failure");
            });
            fail();
        } catch (JsonProviderException expected) {
            assertTrue(expected.getMessage().contains("$[bad]"));
            assertEquals("[{}", output.buffer.toString());
        }
        try {
            output.commit();
            fail();
        } catch (JsonProviderException expected) {
            assertTrue(expected.getMessage().contains("incomplete"));
        } finally {
            output.close();
        }
        assertTrue(output.outputClosed);
        assertEquals("[{}", output.buffer.toString());
    }

    public void testFinalizationFailurePropagates() {
        JsonConnection output = new JsonConnection(new MockConnectionParameters()) {
            protected Writer newOutputWriter() {
                return new Writer() {
                    public void write(char[] value, int offset, int length) { }
                    public void flush() throws IOException { throw new IOException("flush failure"); }
                    public void close() { }
                };
            }
        };
        output.executeScript(new StringResource("{}"), name -> null);
        try {
            output.commit();
            fail();
        } catch (JsonProviderException expected) {
            assertEquals("flush failure", expected.getCause().getMessage());
        } finally {
            output.close();
        }
    }
}
