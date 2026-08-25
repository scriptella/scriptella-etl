/*
 * Copyright 2006-2026 The Scriptella Project Team.
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ConfigurationFactoryValidationTest extends TestCase {
    private final Logger configurationLogger = Logger.getLogger(ConfigurationFactory.class.getName());
    private Handler warningHandler;

    public void testDocumentWithoutDoctypeDoesNotLogValidationWarning() {
        List<String> messages = captureConfigurationWarnings();
        try {
            ConfigurationFactory factory = newFactory("NoDoctypeTest.xml");
            ConfigurationEl configuration = factory.createConfiguration();

            assertEquals(1, configuration.getConnections().size());
            assertTrue(messages.toString(), messages.isEmpty());
        } finally {
            removeHandlers();
        }
    }

    public void testDocumentWithDoctypeUsesBundledDtdValidation() {
        List<String> messages = captureConfigurationWarnings();
        try {
            ConfigurationFactory factory = newFactory("DtdValidationTest.xml");
            factory.createConfiguration();

            assertTrue(messages.toString(), containsMessage(messages, "unexpected"));
        } finally {
            removeHandlers();
        }
    }

    public void testValidationDiagnosticsArePublishedBeforeParseFailure() {
        List<String> messages = captureConfigurationWarnings();
        try {
            ConfigurationFactory factory = newFactory("DtdValidationThenMalformedTest.xml");
            try {
                factory.createConfiguration();
                fail("Malformed XML should fail parsing");
            } catch (ConfigurationException expected) {
                // Expected.
            }

            assertTrue(messages.toString(), containsMessage(messages, "unexpected"));
        } finally {
            removeHandlers();
        }
    }

    private ConfigurationFactory newFactory(String resource) {
        ConfigurationFactory factory = new ConfigurationFactory();
        factory.setResourceURL(getClass().getResource(resource));
        return factory;
    }

    private List<String> captureConfigurationWarnings() {
        final List<String> messages = new ArrayList<String>();
        warningHandler = new Handler() {
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    messages.add(record.getMessage());
                }
            }

            public void flush() {
            }

            public void close() {
            }
        };
        configurationLogger.addHandler(warningHandler);
        return messages;
    }

    private boolean containsMessage(List<String> messages, String text) {
        for (String message : messages) {
            if (message.contains(text)) {
                return true;
            }
        }
        return false;
    }

    private void removeHandlers() {
        if (warningHandler != null) {
            configurationLogger.removeHandler(warningHandler);
            warningHandler.close();
            warningHandler = null;
        }
    }
}
