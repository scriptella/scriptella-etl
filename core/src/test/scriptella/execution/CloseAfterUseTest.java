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
package scriptella.execution;

import junit.framework.TestCase;
import scriptella.spi.*;
import scriptella.util.IOUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Observable driver operations verify ordering without timing or database assumptions. */
public class CloseAfterUseTest extends TestCase {
    private static final List<String> events = new ArrayList<String>();
    private static final String GLOBAL = "scriptella.close.after.use";

    protected void setUp() {
        events.clear();
    }

    private String connection(String id, String properties) {
        return "<connection id='" + id + "' driver='" + Driver.class.getName()
                + "' url='" + id + "'>" + properties + "</connection>";
    }

    private EtlExecutor executor(String body, Map<String, ?> parameters) throws Exception {
        Path file = Files.createTempFile("scriptella-close", ".xml");
        try {
            Files.writeString(file, "<!DOCTYPE etl SYSTEM 'http://scriptella.org/dtd/etl.dtd'><etl>" + body + "</etl>");
            EtlExecutor executor = EtlExecutor.newExecutor(file.toUri().toURL(), parameters);
            executor.setJmxEnabled(false);
            return executor;
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private String scripts() {
        return "<script connection-id='a'>one</script><script connection-id='b'>two</script>";
    }

    public void testDefaultAndExplicitOverride() throws Exception {
        for (Map<String, ?> global : Arrays.<Map<String, ?>>asList(Collections.emptyMap(), Collections.singletonMap(GLOBAL, "yes"))) {
            events.clear();
            executor(connection("a", "close.after.use=false") + connection("b", "") + scripts(), global).execute();
            assertTrue(events.indexOf("b:two") < events.indexOf("a:commit"));
            assertEquals(1, Collections.frequency(events, "a:close"));
        }
        events.clear();
        executor(connection("a", "") + connection("b", "") + scripts(), Collections.emptyMap()).execute();
        assertTrue(events.indexOf("b:two") < events.indexOf("a:commit"));
    }

    public void testGlobalAndLocalEnable() throws Exception {
        for (boolean global : new boolean[]{false, true}) {
            events.clear();
            executor(connection("a", global ? "" : "close.after.use=on") + connection("b", "") + scripts(),
                    global ? Collections.singletonMap(GLOBAL, "true") : Collections.emptyMap()).execute();
            assertTrue(events.indexOf("a:close") < events.indexOf("b:two"));
            assertEquals(1, Collections.frequency(events, "a:commit"));
            assertEquals(1, Collections.frequency(events, "a:close"));
        }
    }

    public void testEtLDefaultAndExternalPrecedence() throws Exception {
        String body = "<properties>scriptella.close.after.use=true</properties>"
                + connection("a", "") + connection("b", "") + scripts();
        executor(body, Collections.emptyMap()).execute();
        assertTrue(events.indexOf("a:close") < events.indexOf("b:two"));
        events.clear();
        executor(body, Collections.singletonMap(GLOBAL, false)).execute();
        assertTrue(events.indexOf("b:two") < events.indexOf("a:close"));
    }

    public void testMultipleReferencesNestedIterationsNewTxAndCounters() throws Exception {
        ExecutionProgress progress = new ExecutionProgress();
        ExecutionStatistics stats = executor(connection("a", "close.after.use=true") + connection("b", "")
                + "<script connection-id='a'>one</script>"
                + "<query connection-id='a'>rows<script>nested</script>"
                + "<script new-tx='true'>separate</script></query>"
                + "<script connection-id='b'>two</script>", Collections.emptyMap()).execute(null, progress);
        assertEquals(2, Collections.frequency(events, "a:nested"));
        assertEquals(2, Collections.frequency(events, "a:separate"));
        assertTrue(events.indexOf("a:query-end") < events.indexOf("a:close"));
        assertTrue(events.lastIndexOf("a:close") < events.indexOf("b:two"));
        assertEquals(2, Collections.frequency(events, "a:open"));
        assertEquals(2, Collections.frequency(events, "a:close"));
        assertEquals(7L, progress.snapshot().getStatements());
        assertEquals(7, stats.getExecutedStatementsCount());
        assertEquals(7L, stats.getUpdateCount());
    }

    public void testHandlersAndConditionsRemainPotentialUses() throws Exception {
        executor(connection("a", "close.after.use=true") + connection("b", "")
                + "<script connection-id='a'>one</script>"
                + "<script connection-id='b'>fail<onerror connection-id='a'>handled</onerror></script>"
                + "<script connection-id='a' if='false'>skipped</script>"
                + "<script connection-id='b'>two</script>", Collections.emptyMap()).execute();
        assertTrue(events.indexOf("a:handled") < events.indexOf("a:close"));
        assertTrue(events.indexOf("a:close") < events.indexOf("b:two"));
    }

    public void testSkippedElementWithoutConnectionId() throws Exception {
        EtlExecutor executor = executor(connection("a", "close.after.use=true") + connection("b", "")
                + "<script connection-id='a'>one</script>"
                + "<script connection-id='b' if='false'>skipped</script>"
                + "<script connection-id='b'>two</script>", Collections.emptyMap());
        // XML validation requires an ID; the mutable configuration API can omit it.
        executor.getConfiguration().getScriptingElements().get(1).setConnectionId(null);
        executor.execute();
        assertTrue(events.contains("a:one"));
        assertTrue(events.contains("b:two"));
        assertFalse(events.contains("a:skipped"));
        assertFalse(events.contains("b:skipped"));
        assertTrue(events.indexOf("a:close") < events.indexOf("b:two"));
    }

    public void testExecutedElementWithoutConnectionIdStillFailsAtRuntime() throws Exception {
        try {
            EtlExecutor executor = executor(connection("a", "close.after.use=true") + connection("b", "")
                    + "<script connection-id='a'>one</script>"
                    + "<script connection-id='b' if='true'>missing</script>", Collections.emptyMap());
            executor.getConfiguration().getScriptingElements().get(1).setConnectionId(null);
            executor.execute();
            fail("Expected missing connection ID failure");
        } catch (EtlExecutorException expected) {
            // The preceding script ran: preparation did not reject the element.
            assertTrue(events.contains("a:one"));
            assertFalse(events.contains("a:missing"));
            assertFalse(events.contains("b:missing"));
        }
    }

    public void testUnopenedLazyConnection() throws Exception {
        String lazy = connection("a", "close.after.use=true").replace("id='a'", "id='a' lazy-init='true'");
        executor(lazy + connection("b", "") + "<script connection-id='b'>two</script>", Collections.emptyMap()).execute();
        assertFalse(events.contains("a:open"));
    }

    public void testFailureCancellationAndCommitFailure() throws Exception {
        for (String action : new String[]{"fail", "cancel", "badcommit"}) {
            events.clear();
            ExecutionProgress progress = new ExecutionProgress();
            String body = connection("a", "close.after.use=true") + connection("b", "")
                    + "<script connection-id='a'>" + (action.equals("badcommit") ? action : "one") + "</script>"
                    + "<script connection-id='b'>" + action + "</script>";
            try {
                executor(body, Collections.emptyMap()).execute(null, progress);
                fail("Expected " + action);
            } catch (EtlExecutorException expected) {
                assertEquals(action.equals("cancel"), expected.isCancelled());
            } finally {
                Thread.interrupted();
            }
            assertEquals(1, Collections.frequency(events, "a:close"));
            assertEquals(1, Collections.frequency(events, "b:close"));
            assertTrue(events.contains("b:rollback"));
            assertEquals(action.equals("badcommit"), events.contains("a:rollback"));
            assertTrue(progress.snapshot().getStatements() >= 1);
        }
    }

    public void testJdbcBatchesCommitBeforeLaterFailureAndDefaultRollsBack() throws Exception {
        String url = "jdbc:h2:mem:closeAfterUse" + System.nanoTime();
        try (java.sql.Connection observer = java.sql.DriverManager.getConnection(url);
             java.sql.Statement sql = observer.createStatement()) {
            sql.execute("CREATE TABLE result(id INT)");
            for (boolean enabled : new boolean[]{false, true}) {
                ExecutionProgress progress = new ExecutionProgress();
                String body = "<connection id='db' driver='org.h2.Driver' url='" + url + "'>"
                        + "close.after.use=" + enabled + "\nstatement.batchSize=3</connection>"
                        + connection("other", "")
                        + "<script connection-id='db'>INSERT INTO result VALUES (1);"
                        + "INSERT INTO result VALUES (2);</script>"
                        + "<script connection-id='other'>fail</script>";
                try {
                    executor(body, Collections.emptyMap()).execute(null, progress);
                    fail("Expected later failure");
                } catch (EtlExecutorException expected) {
                    assertTrue(progress.snapshot().getStatements() >= 2);
                }
                try (java.sql.ResultSet rows = sql.executeQuery("SELECT COUNT(*) FROM result")) {
                    assertTrue(rows.next());
                    assertEquals(enabled ? 2 : 0, rows.getInt(1));
                }
            }
            ExecutionStatistics stats = executor("<connection id='db' driver='org.h2.Driver' url='" + url
                    + "'>close.after.use=true\nstatement.batchSize=3</connection>"
                    + connection("other", "")
                    + "<script connection-id='db'>INSERT INTO result VALUES (3)</script>"
                    + "<script connection-id='other'>two</script>", Collections.emptyMap()).execute();
            assertEquals(2L, stats.getUpdateCount()); // one JDBC update plus the test driver's one
            assertEquals(2, stats.getExecutedStatementsCount());
        }
    }

    public void testFailureInsideFinalSubtreeDoesNotCommit() throws Exception {
        for (String action : new String[]{"fail", "cancel"}) {
            events.clear();
            try {
                executor(connection("a", "close.after.use=true") + connection("b", "close.after.use=true")
                        + "<query connection-id='a'>rows<script connection-id='b'>" + action
                        + "</script></query>", Collections.emptyMap()).execute();
                fail("Expected failure");
            } catch (EtlExecutorException expected) {
                assertFalse(events.contains("a:commit"));
                assertFalse(events.contains("b:commit"));
                assertTrue(events.contains("a:rollback"));
                assertTrue(events.contains("b:rollback"));
                assertEquals(1, Collections.frequency(events, "a:close"));
                assertEquals(1, Collections.frequency(events, "b:close"));
            } finally {
                Thread.interrupted();
            }
        }
    }

    public void testInvalidBoolean() throws Exception {
        try {
            executor(connection("a", "close.after.use=maybe") + "<script>one</script>", Collections.emptyMap()).execute();
            fail("Expected invalid boolean");
        } catch (EtlExecutorException expected) {
            assertFalse(events.contains("a:open"));
        }
    }

    public static class Driver implements ScriptellaDriver {
        public scriptella.spi.Connection connect(ConnectionParameters parameters) {
            return new TestConnection(parameters.getUrl());
        }
    }

    private static class TestConnection extends AbstractConnection {
        private final String id;
        private boolean closed;
        private boolean badCommit;

        TestConnection(String id) {
            this.id = id;
            events.add(id + ":open");
            setDialectIdentifier(new DialectIdentifier("test", "1"));
        }

        public StatementCounter getProgressCounter() { return counter; }
        public long getUpdateCount() { return counter.statements; }

        public void executeScript(Resource resource, ParametersCallback parameters) {
            assertFalse(closed);
            String text;
            try { text = IOUtils.toString(resource.open()).trim(); }
            catch (Exception e) { throw new IllegalStateException(e); }
            events.add(id + ":" + text);
            counter.statements++;
            if (text.equals("fail")) { throw new IllegalStateException("test failure"); }
            if (text.equals("cancel")) {
                Thread.currentThread().interrupt();
                throw new scriptella.core.EtlCancelledException();
            }
            badCommit = text.equals("badcommit");
        }

        public void executeQuery(Resource resource, ParametersCallback parameters, QueryCallback callback) {
            assertFalse(closed);
            callback.processRow(parameters);
            callback.processRow(parameters);
            counter.statements++;
            events.add(id + ":query-end");
        }

        public void commit() {
            assertFalse(closed);
            events.add(id + ":commit");
            if (badCommit) { throw new IllegalStateException("commit failure"); }
        }
        public void rollback() { assertFalse(closed); events.add(id + ":rollback"); }
        public void close() { assertFalse(closed); closed = true; events.add(id + ":close"); }
    }
}
