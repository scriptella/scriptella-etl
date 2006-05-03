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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.net.URL;
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
    private static final String DTD_NAME = "scriptella.dtd";

    static {
        DBF.setValidating(true);
    }

    private URL resourceURL;

    public ConfigurationFactory() {
    }

    public URL getResourceURL() {
        return resourceURL;
    }

    public void setResourceURL(final URL resourceURL) {
        this.resourceURL = resourceURL;
    }

    public ConfigurationEl createConfiguration() {
        try {
            DocumentBuilder db = DBF.newDocumentBuilder();
            db.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(final String publicId,
                                                 final String systemId) {
                    if (systemId.endsWith(DTD_NAME)) {
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

            return new ConfigurationEl(new XMLElement(
                    document.getDocumentElement(), resourceURL));
        } catch (IOException e) {
            throw new ConfigurationException("Unable to load document: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ConfigurationException("Unable to parse document: " + e.getMessage(), e);
        }
    }
}
