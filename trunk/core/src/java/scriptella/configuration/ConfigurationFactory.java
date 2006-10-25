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

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import scriptella.core.ThreadSafe;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConfigurationFactory {
    private static final Logger LOG = Logger.getLogger(ConfigurationFactory.class.getName());
    private static final DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance();
    private static final String DTD_NAME = "etl.dtd";
    private URL resourceURL;
    private Map<String, ?> externalProperties;

    static {
        setValidating(true);
    }


    /**
     * Sets validation option.
     *
     * @param validating true if XML file validation should be performed.
     */
    public static void setValidating(boolean validating) {
        DBF.setValidating(validating);
    }

    public ConfigurationFactory() {
    }

    public URL getResourceURL() {
        return resourceURL;
    }

    public void setResourceURL(final URL resourceURL) {
        this.resourceURL = resourceURL;
    }

    /**
     * A getter for external Properties.
     *
     * @return external properties set by {@link #setExternalProperties}.
     */
    @ThreadSafe
    public Map<String, ?> getExternalProperties() {
        return externalProperties;
    }

    /**
     * Sets additional properties.
     * <p>External properties takes precedence over properties specified
     * in ETL &lt;properties&gt; element.
     * <p>Intended for integration with other systems like ant.
     *
     * @param externalProperties external properties. Nulls allowed.
     */
    @ThreadSafe
    public void setExternalProperties(final Map<String, ?> externalProperties) {
        if (externalProperties==null) {
            this.externalProperties=null;
        } else {
            this.externalProperties = new LinkedHashMap<String, Object>(externalProperties);
        }
    }

    public ConfigurationEl createConfiguration() {
        try {
            DocumentBuilder db = DBF.newDocumentBuilder();
            db.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(final String publicId,
                                                 final String systemId) {
                    if (systemId != null && systemId.trim().endsWith(DTD_NAME)) {
                        return new InputSource(ConfigurationFactory.class.getResourceAsStream(
                                "/scriptella/dtd/" + DTD_NAME));
                    }

                    return null;
                }
            });
            db.setErrorHandler(new ErrorHandler() {
                public void warning(final SAXParseException exception) {
                    LOG.warning(messageFor(exception));
                }

                public void error(final SAXParseException exception) {
                    LOG.warning(messageFor(exception));
                }

                private String messageFor(final SAXParseException exception) {
                    StringBuilder sb = new StringBuilder(32);
                    sb.append("XML configuration warning in ");

                    final String sid = exception.getSystemId();

                    if (sid != null) {
                        sb.append(sid);
                    } else {
                        sb.append("the document");
                    }

                    sb.append('(');
                    sb.append(exception.getLineNumber());
                    sb.append(':');
                    sb.append(exception.getColumnNumber());
                    sb.append("): ");
                    sb.append(exception.getMessage());

                    return sb.toString();
                }

                public void fatalError(final SAXParseException exception) {
                    LOG.severe(messageFor(exception));
                }
            });

            final InputSource inputSource = new InputSource(resourceURL.toString());
            final Document document = db.parse(inputSource);
            PropertiesMerger merger = externalProperties == null ?
                    new PropertiesMerger() : new PropertiesMerger(externalProperties);

            return new ConfigurationEl(new XmlElement(
                    document.getDocumentElement(), resourceURL, merger.getSubstitutor()), merger);
        } catch (IOException e) {
            throw new ConfigurationException("Unable to load document: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ConfigurationException("Unable to parse document: " + e.getMessage(), e);
        }
    }
}
