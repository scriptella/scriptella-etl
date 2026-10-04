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
package scriptella.configuration;

import junit.framework.TestCase;
import scriptella.spi.support.MapParametersCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProgrammaticConfigurationTest extends TestCase {
    public void testNullConnectionPropertiesAreEmpty() {
        ConnectionEl connection = new ConnectionEl("jdbc:h2:mem:validation", null, null, null);
        assertNotNull(connection.getProperties());
        assertTrue(connection.getProperties().isEmpty());
    }

    public void testRejectsEmptyConnections() {
        assertInvalid(Collections.<ConnectionEl>emptyList(), Collections.<ScriptingElement>emptyList(),
                "At least one connection");
    }

    public void testRejectsDuplicateConnectionIds() {
        assertInvalid(List.of(connection("same"), connection("same")), List.of(script("same")),
                "Connection ID must be unique");
        assertInvalid(List.of(connection(null), connection(null)), List.of(script(null)),
                "Connection ID is required");
    }

    public void testRejectsMissingConnectionAndScriptIds() {
        assertInvalid(List.of(connection("first"), connection(null)), List.of(script("first")),
                "Connection ID is required");
        assertInvalid(List.of(connection("first"), connection("second")), List.of(script(null)),
                "connection-id is a required attribute");
    }

    public void testRejectsUnknownScriptConnectionIds() {
        assertInvalid(List.of(connection("first")), List.of(script("unknown")), "invalid connection-id");
        assertInvalid(List.of(connection("first"), connection("second")), List.of(script("unknown")),
                "invalid connection-id");
    }

    public void testValidConfigurationsAndListCopies() {
        List<ConnectionEl> connections = new ArrayList<ConnectionEl>(List.of(connection(null)));
        List<ScriptingElement> scripts = new ArrayList<ScriptingElement>(List.of(script(null)));
        ConfigurationEl configuration = create(connections, scripts);
        connections.clear();
        scripts.clear();
        assertEquals(1, configuration.getConnections().size());
        assertEquals(1, configuration.getScriptingElements().size());
        create(List.of(connection("first"), connection("second")), List.of(script("first"), script("second")));
    }

    private void assertInvalid(List<ConnectionEl> connections, List<ScriptingElement> scripts, String message) {
        try {
            create(connections, scripts);
            fail("Expected invalid configuration");
        } catch (ConfigurationException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(message));
        }
    }

    private ConfigurationEl create(List<ConnectionEl> connections, List<ScriptingElement> scripts) {
        return new ConfigurationEl(null, new MapParametersCallback(Collections.emptyMap()), connections, scripts);
    }

    private ConnectionEl connection(String id) {
        ConnectionEl connection = new ConnectionEl("jdbc:h2:mem:validation", null, null, Collections.emptyMap());
        connection.setId(id);
        return connection;
    }

    private ScriptEl script(String id) {
        ScriptEl script = new ScriptEl(new StringResource("SELECT 1"));
        script.setConnectionId(id);
        return script;
    }
}
