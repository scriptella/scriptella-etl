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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class EnvironmentConfigurationTest extends TestCase {
    public void testPropertiesFailuresKeepTheirWrapperAndXmlContext() throws Exception {
        for (final ConfigurationException failure : new ConfigurationException[]{
                new ConfigurationException("ordinary configuration failure"),
                new MissingEnvironmentVariableException("SCRIPTELLA_MISSING_TEST")}) {
            org.w3c.dom.Document document = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
            org.w3c.dom.Element properties = document.createElement("properties");
            properties.appendChild(document.createTextNode("foo=bar"));
            XmlElement element = new XmlElement(properties, null, null) {
                @Override
                public String expandProperties(String value) {
                    throw failure;
                }
            };
            try {
                new PropertiesEl(element);
                fail("Expected properties failure");
            } catch (ConfigurationException expected) {
                String message = failure instanceof MissingEnvironmentVariableException ?
                        "Unable to load properties: " + failure.getMessage() : "Unable to load properties";
                assertEquals(message, expected.getMessage());
                assertSame(failure, expected.getCause());
                java.lang.reflect.Field context = ConfigurationException.class.getDeclaredField("element");
                context.setAccessible(true);
                assertSame(element, context.get(expected));
            }
        }
    }

    public void testMissingEnvironmentNamesAreVisibleInConfigurationErrors() throws Exception {
        String name = "SCRIPTELLA_MISSING_65_" + java.util.UUID.randomUUID().toString().replace("-", "");
        Path file = Files.createTempFile("scriptella-missing-env", ".xml");
        try {
            for (String xml : new String[]{
                    "<etl><properties>password=${env." + name + "}</properties>" +
                            "<connection driver='jdbc' url='jdbc:h2:mem:test'/><script>SELECT 1;</script></etl>",
                    "<etl><connection driver='jdbc' url='jdbc:h2:mem:test' password='${env." + name +
                            "}'/><script>SELECT 1;</script></etl>"}) {
                Files.writeString(file, xml);
                ConfigurationFactory factory = new ConfigurationFactory();
                factory.setResourceURL(file.toUri().toURL());
                try {
                    factory.createConfiguration();
                    fail("Expected missing environment configuration error");
                } catch (ConfigurationException expected) {
                    assertTrue(expected.getMessage(), expected.getMessage().contains("Missing environment variable: " + name));
                }
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    public void testEnvironmentAndExistingPrecedence() throws Exception {
        String name = System.getenv("PATH") == null ? "Path" : "PATH";
        assertNotNull("Process search path must be defined", System.getenv(name));
        String reference = "${env." + name + "}";
        Path file = Files.createTempFile("scriptella-env", ".xml");
        try {
            Files.writeString(file, "<etl><properties>foo=local\nenv=shadow\nenv." + name +
                    "=shadow</properties><connection driver='jdbc' url='${foo}' user='" + reference +
                    "' password='" + reference + "'/><script>SELECT 1;</script></etl>");
            ConfigurationFactory factory = new ConfigurationFactory();
            factory.setResourceURL(file.toUri().toURL());
            Map<String, Object> external = new HashMap<String, Object>();
            external.put("foo", "jdbc:h2:mem:external");
            external.put("env", "external shadow");
            external.put("env." + name, "external shadow");
            factory.setExternalParameters(external);
            ConnectionEl connection = factory.createConfiguration().getConnections().get(0);
            assertEquals("jdbc:h2:mem:external", connection.getUrl());
            assertEquals(System.getenv(name), connection.getUser());
            assertEquals(System.getenv(name), connection.getPassword());
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
