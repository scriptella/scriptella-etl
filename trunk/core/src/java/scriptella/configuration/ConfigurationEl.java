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

import scriptella.spi.ParametersCallback;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConfigurationEl extends XmlConfigurableBase {
    private List<ConnectionEl> connections;
    private List<ScriptingElement> scriptingElements;
    private PropertiesMerger propertiesMerger;
    private URL documentUrl;

    public ConfigurationEl(XmlElement element, PropertiesMerger merger) {
        propertiesMerger = merger;
        configure(element);
    }

    public List<ConnectionEl> getConnections() {
        return connections;
    }

    public void setConnections(final List<ConnectionEl> connections) {
        this.connections = connections;
    }

    public List<ScriptingElement> getScriptingElements() {
        return scriptingElements;
    }

    public void setScriptingElements(final List<ScriptingElement> scriptingElements) {
        this.scriptingElements = scriptingElements;
    }

    /**
     * Returns this configuration properties merged with external ones specified in a factory. 
     */
    public ParametersCallback getProperties() {
        return propertiesMerger;
    }

    public URL getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(final URL documentUrl) {
        this.documentUrl = documentUrl;
    }

    public void configure(final XmlElement element) {
        documentUrl = element.getDocumentURL();

        Map<String,?> xmlProps = new PropertiesEl(element.getChild("properties")).getMap();
        //Now merge external and local xml properties
        propertiesMerger.addProperties(xmlProps);

        setConnections(load(element.getChildren("connection"),
                ConnectionEl.class));
        if (connections.isEmpty()) {
            throw new ConfigurationException("At least one connection element must be declared", element);
        }
        scriptingElements = QueryEl.loadScriptingElements(element, null);
        validateScriptingElements(element);
    }

    void validateScriptingElements(final XmlElement element) {
        //validating scriptingElements
        Set<String> allowedConIds = new HashSet<String>();
        for (ConnectionEl connectionEl : connections) {
            final String cid = connectionEl.getId();
            if (!allowedConIds.add(cid)) {
                throw new ConfigurationException("Connection ID must be unique for ETL file", element);
            }
            if (cid==null && connections.size()>1) {
                throw new ConfigurationException("Connection ID is required if more than one connection specified in ETL script.", element);
            }
        }

        validateScriptingElements(allowedConIds, element, scriptingElements);
    }

    void validateScriptingElements(final Set<String> allowedConIds, final XmlElement element, final List<ScriptingElement> elements) {
        for (ScriptingElement se : elements) {
            //If one connection check
            final int allowedConSize = allowedConIds.size();
            final String seConnectionId = se.getConnectionId();
            if (allowedConSize == 1 && seConnectionId != null &&
                    !allowedConIds.contains(seConnectionId)) {
                throw new ConfigurationException("Element " + se.getLocation() + " has invalid connection-id");
            } else if (allowedConSize > 1 && seConnectionId == null && scriptingElements==elements) {
                //Nulls are allowed only in nested scripts and queries
                throw new ConfigurationException("connection-id is a required attribute for element " + se.getLocation());
            } else
            if (allowedConSize > 1 && seConnectionId != null && !allowedConIds.contains(seConnectionId))
            {
                throw new ConfigurationException("Element " + se.getLocation() + " has invalid connection-id");
            }
            if (se instanceof QueryEl) {
                validateScriptingElements(allowedConIds, element, ((QueryEl) se).getChildScriptinglElements());
            }
        }
    }

    public String toString() {
        return "ConfigurationEl{" + "connections=" + connections +
                ", scriptingElements=" + scriptingElements + ", properties=" + propertiesMerger +
                ", documentUrl=" + documentUrl + "}";
    }
}
