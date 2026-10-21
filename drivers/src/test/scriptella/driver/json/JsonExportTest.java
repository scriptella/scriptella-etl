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
import scriptella.execution.EtlExecutor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Runs real XML resources, dynamic includes, JDBC rows and eager empty exports. */
public class JsonExportTest extends TestCase {
    public void testExportsAndDynamicIncludes() throws Exception {
        Path directory = Files.createTempDirectory("json-export");
        try {
            Files.writeString(directory.resolve("first.json"), "{\"first\":?{id},\"credit\":?{credit},\"literal\":\"${id} ?{id}\"}");
            Files.writeString(directory.resolve("second.json"), "{\"second\":?{id},\"credit\":?{credit},\"literal\":\"${id} ?{id}\"}");
            Files.writeString(directory.resolve("existing.json"), "untouched");
            Files.writeString(directory.resolve("input.json"), "{\"response\":{\"customers\":[{\"id\":1,\"items\":[{\"price\":12.50},null]}]}}");
            Path etl = directory.resolve("etl.xml");
            try (java.io.InputStream fixture = getClass().getResourceAsStream("JsonExportTest.xml")) {
                assertNotNull("Missing ETL fixture", fixture);
                Files.copy(fixture, etl);
            }
            EtlExecutor.newExecutor(etl.toFile()).execute();
            assertEquals("[{\"first\":1,\"credit\":12.50,\"literal\":\"${id} ?{id}\"},"
                    + "{\"second\":2,\"credit\":7.25,\"literal\":\"${id} ?{id}\"}]",
                    Files.readString(directory.resolve("export.json")));
            assertEquals("[{\"id\":1,\"items\":[{\"price\":12.50},null],\"note\":null,\"literal\":\"${row.id}\"}]",
                    Files.readString(directory.resolve("transformed.json")));
            assertEquals("untouched", Files.readString(directory.resolve("existing.json")));
            assertFalse(Files.exists(directory.resolve("absent.json")));
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                    Files.delete(path);
                }
            }
        }
    }
}
