/*
 * Copyright 2006 The Scriptella Project Team.
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

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConfigurationEl extends XMLConfigurableBase {
    private List<ConnectionEl> connections;
    private List<SQLBasedElement> sqlElements;
    private PropertiesEl properties;
    private URL documentUrl;

    public ConfigurationEl(XMLElement element) {
        configure(element);
    }

    public List<ConnectionEl> getConnections() {
        return connections;
    }

    public void setConnections(final List<ConnectionEl> connections) {
        this.connections = connections;
    }

    public List<SQLBasedElement> getSqlElements() {
        return sqlElements;
    }

    public void setSqlElements(final List<SQLBasedElement> sqlElements) {
        this.sqlElements = sqlElements;
    }

    public PropertiesEl getProperties() {
        return properties;
    }

    public void setProperties(final PropertiesEl properties) {
        this.properties = properties;
    }

    public URL getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(final URL documentUrl) {
        this.documentUrl = documentUrl;
    }

    public void configure(final XMLElement element) {
        documentUrl = element.getDocumentURL();
        properties = new PropertiesEl(element.getChild("properties"));

        Map<String, String> allProps = new HashMap<String, String>();

        setConnections(load(element.getChildren("connection"),
                ConnectionEl.class));
        sqlElements = QueryEl.loadSqlElements(element);
    }

    public String toString() {
        return "ConfigurationEl{" + "connections=" + connections +
                ", sqlElements=" + sqlElements + ", properties=" + properties +
                ", documentUrl=" + documentUrl + "}";
    }
}
