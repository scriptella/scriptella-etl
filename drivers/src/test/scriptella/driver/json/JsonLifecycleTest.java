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
import java.io.StringWriter;
import java.io.Writer;

/** Controlled failures at each output lifecycle boundary. */
public class JsonLifecycleTest extends TestCase {
    private static class Output extends JsonConnection {
        final StringWriter bytes = new StringWriter();
        String failure;
        boolean cleanupFailure;
        int closes;
        int remaining = Integer.MAX_VALUE;

        Output() {
            super(new MockConnectionParameters());
        }

        protected Writer newOutputWriter() throws IOException {
            if ("open".equals(failure)) {
                throw new IOException("open failure");
            }
            return new Writer() {
                public void write(char[] value, int offset, int length) throws IOException {
                    for (int i = 0; i < length; i++) {
                        if (remaining-- == 0) {
                            throw new IOException("write failure");
                        }
                        bytes.write(value[offset + i]);
                    }
                }
                public void flush() throws IOException {
                    if ("flush".equals(failure)) {
                        throw new IOException("flush failure");
                    }
                }
                public void close() throws IOException {
                    closes++;
                    if (cleanupFailure || "close".equals(failure)) {
                        throw new IOException("close failure");
                    }
                }
            };
        }
    }

    private void record(Output output) {
        output.executeScript(new StringResource("{}"), name -> null);
    }

    private void assertFailed(Output output) {
        try {
            output.commit();
            fail("Failed export committed");
        } catch (JsonProviderException expected) {
            assertTrue(expected.getMessage().contains("incomplete"));
        }
        try {
            record(output);
            fail("Failed export accepted another record");
        } catch (JsonProviderException expected) {
            assertTrue(expected.getMessage().contains("failed"));
        }
    }

    public void testOpenAndEveryRecordWriteBoundary() {
        // Opening bracket, first record, separator, and second record.
        for (int boundary = -1; boundary < 6; boundary++) {
            Output output = new Output();
            if (boundary == -1) {
                output.failure = "open";
            } else {
                output.remaining = boundary;
            }
            try {
                record(output);
                record(output);
                fail("Expected injected failure");
            } catch (JsonProviderException expected) {
                assertTrue(expected.getMessage().contains("record "));
                assertTrue(expected.getCause().getMessage().contains("failure"));
                assertEquals(boundary < 3 ? 0L : 1L, output.getExecutedStatementsCount());
            }
            assertFailed(output);
            output.close();
            assertEquals(boundary == -1 ? 0 : 1, output.closes);
            assertFalse(output.bytes.toString().endsWith("]"));
        }
    }

    public void testFinalizationFailuresAndSuppressedCleanup() {
        for (String failure : new String[]{"write", "flush", "close"}) {
            Output output = new Output();
            record(output);
            output.failure = failure;
            output.cleanupFailure = true;
            if ("write".equals(failure)) {
                output.remaining = 0;
            }
            try {
                output.commit();
                fail();
            } catch (JsonProviderException expected) {
                assertEquals(failure + " failure", expected.getCause().getMessage());
                assertTrue(expected.getMessage().contains("record 1 during finalization"));
                assertEquals("close".equals(failure) ? 0 : 1,
                        expected.getCause().getSuppressed().length);
            }
            assertEquals(1, output.closes);
            assertFailed(output);
            output.close();
            output.close();
            assertEquals(1, output.closes);
        }
    }

    public void testCancellationAndRollbackLeavePartialExport() {
        for (boolean cancel : new boolean[]{true, false}) {
            Output output = new Output();
            record(output);
            if (cancel) {
                Thread.currentThread().interrupt();
                try {
                    record(output);
                    fail();
                } catch (EtlCancelledException expected) {
                    // Preserve the engine's cancellation signal.
                } finally {
                    Thread.interrupted();
                }
            } else {
                output.rollback();
            }
            assertFailed(output);
            output.close();
            assertEquals("[{}", output.bytes.toString());
            assertEquals(1, output.closes);
        }
    }

    public void testCancellationDuringEvaluationNeverAppendsRecord() {
        Output output = new Output();
        record(output);
        try {
            output.executeScript(new StringResource("{\"value\":?{value}}"), name -> {
                Thread.currentThread().interrupt();
                return 1;
            });
            fail();
        } catch (EtlCancelledException expected) {
            assertEquals("[{}", output.bytes.toString());
        } finally {
            Thread.interrupted();
        }
        assertFailed(output);
        output.close();
        assertEquals(1, output.closes);
    }

    public void testCleanupFailureDoesNotFinalizeAndCloseIsIdempotent() {
        Output output = new Output();
        record(output);
        output.cleanupFailure = true;
        try {
            output.close();
            fail();
        } catch (JsonProviderException expected) {
            assertEquals("close failure", expected.getCause().getMessage());
        }
        output.close();
        assertEquals(1, output.closes);
        assertEquals("[{}", output.bytes.toString());
    }
    public static class InjectedDriver extends Driver {
        static Output connection;

        public scriptella.spi.Connection connect(scriptella.spi.ConnectionParameters parameters) {
            return connection;
        }
    }

    public void testHandledFailuresStillFailEtlAndClosePartialExport() throws Exception {
        for (String failure : new String[]{"open", "write", "write-cleanup", "mapping", "flush", "close", "downstream"}) {
            Output output = new Output();
            InjectedDriver.connection = output;
            java.nio.file.Path directory = java.nio.file.Files.createTempDirectory("json-failure");
            try {
                if ("open".equals(failure)) {
                    output.failure = failure;
                } else if (failure.startsWith("write")) {
                    output.cleanupFailure = "write-cleanup".equals(failure);
                    output.remaining = 4; // First object succeeds; second emission fails.
                } else {
                    output.failure = failure;
                }
                String bad = "mapping".equals(failure) ? "{broken}" : "{}";
                String xml = "<!DOCTYPE etl SYSTEM \"http://scriptella.org/dtd/etl.dtd\"><etl>"
                        + "<connection id=\"out\" driver=\"" + InjectedDriver.class.getName() + "\"/>"
                        + "<connection id=\"handler\" driver=\"json\" url=\"handled.json\"/>"
                        + "<script connection-id=\"out\">{}<onerror connection-id=\"handler\">{}</onerror></script>"
                        + "<script connection-id=\"out\">" + bad
                        + "<onerror connection-id=\"handler\">{}</onerror></script>"
                        + ("downstream".equals(failure)
                        ? "<script connection-id=\"handler\">{broken}</script>" : "")
                        + "</etl>";
                java.nio.file.Path etl = directory.resolve("etl.xml");
                java.nio.file.Files.writeString(etl, xml);
                try {
                    scriptella.execution.EtlExecutor.newExecutor(etl.toFile()).execute();
                    fail("ETL reported success after " + failure);
                } catch (scriptella.execution.EtlExecutorException expected) {
                    assertNotNull(expected.getCause());
                    if ("write-cleanup".equals(failure)) {
                        // The handled write poisons commit; cleanup must not replace that failure.
                        Throwable cause = expected;
                        while (cause != null && (cause.getMessage() == null
                                || !cause.getMessage().contains("incomplete"))) {
                            cause = cause.getCause();
                        }
                        assertNotNull("Cleanup hid the incomplete-export failure", cause);
                    }
                }
                assertEquals("open".equals(failure) ? 0 : 1, output.closes);
                if ("mapping".equals(failure) || failure.startsWith("write")
                        || "downstream".equals(failure)) {
                    assertFalse(output.bytes.toString().endsWith("]"));
                }
                if ("open".equals(failure) || failure.startsWith("write") || "mapping".equals(failure)) {
                    // A handler record proves the original script error was handled.
                    assertTrue(java.nio.file.Files.readString(directory.resolve("handled.json")).contains("{}"));
                }
            } finally {
                InjectedDriver.connection = null;
                try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(directory)) {
                    for (java.nio.file.Path path : (Iterable<java.nio.file.Path>)
                            paths.sorted(java.util.Comparator.reverseOrder())::iterator) {
                        java.nio.file.Files.delete(path);
                    }
                }
            }
        }
    }

}
