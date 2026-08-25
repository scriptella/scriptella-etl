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

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.ParametersCallback;
import scriptella.spi.support.HierarchicalParametersCallback;
import scriptella.spi.support.MapParametersCallback;
import scriptella.spi.support.NullParametersCallback;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;


/**
 * Factory class for ETL {@link ConfigurationEl configuration} files.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConfigurationFactory {
    private static final Logger LOG = Logger.getLogger(ConfigurationFactory.class.getName());
    private static final String DTD_NAME = "etl.dtd";
    private static final String DOCTYPE_DECLARATION = "<!DOCTYPE";
    private static final String RES_PATH = "/scriptella/dtd/" + DTD_NAME;
    private static volatile boolean validating;
    private URL resourceURL;
    private ParametersCallback externalParameters;

    static {
        setValidating(true);
    }


    /**
     * Sets validation option.
     *
     * @param validating true if XML file validation should be performed.
     */
    public static void setValidating(boolean validating) {
        ConfigurationFactory.validating = validating;
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
     * Sets additional properties.
     * <p>External properties takes precedence over properties specified
     * in ETL &lt;properties&gt; element.
     * <p>Intended for integration with other systems like ant.
     *
     * @param externalProperties external properties. Nulls allowed.
     */
    public void setExternalParameters(final Map<String, ?> externalProperties) {
        setExternalParameters(externalProperties == null ? null : new MapParametersCallback(externalProperties));
    }

    /**
     * Sets additional parameters.
     * <p>These parameters take precedence over properties specified in the &lt;properties&gt; section of an ETL file.
     * <p>Intended for integration with other systems like ant.
     *
     * @param externalParameters external parameters.
     */
    public void setExternalParameters(final ParametersCallback externalParameters) {
        this.externalParameters = externalParameters;
    }


    /**
     * Parses XML file and creates a configuration based on a specified parameters.
     *
     * @return configuration element.
     */
    public ConfigurationEl createConfiguration() {
        if (resourceURL == null) {
            throw new ConfigurationException("Configuration URL is required");
        }
        try {
            final Document document = parse(validationRequired());
            HierarchicalParametersCallback params = new HierarchicalParametersCallback(
                    externalParameters == null ? NullParametersCallback.INSTANCE : externalParameters, null);
            PropertiesSubstitutor ps = new PropertiesSubstitutor(params);

            return new ConfigurationEl(new XmlElement(
                    document.getDocumentElement(), resourceURL, ps), params);
        } catch (IOException e) {
            throw new ConfigurationException("Unable to load document: " + e, e);
        } catch (Exception e) {
            throw new ConfigurationException("Unable to parse document: " + e, e);
        }
    }

    private Document parse(final boolean validating) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(validating);

        DocumentBuilder db = factory.newDocumentBuilder();
        db.setEntityResolver(ETL_ENTITY_RESOLVER);
        db.setErrorHandler(ETL_ERROR_HANDLER);

        return db.parse(new InputSource(resourceURL.toString()));
    }

    /**
     * Returns whether validation should be enabled for this document.
     * Only ordinary local files are probed; other URL types retain the historical behavior.
     */
    private boolean validationRequired() throws IOException {
        if (!validating || !"file".equals(resourceURL.getProtocol())) {
            return validating;
        }

        return !isDefinitelyWithoutDoctype();
    }

    /**
     * Performs a lightweight DOCTYPE probe without parsing the document.
     * Returns false when the encoding is uncertain so validation remains enabled.
     */
    private boolean isDefinitelyWithoutDoctype() throws IOException {
        byte[] buffer = new byte[1024];
        int doctypeMatched = 0;

        try (InputStream stream = resourceURL.openStream()) {
            for (int length; (length = stream.read(buffer)) >= 0;) {
                for (int i = 0; i < length; i++) {
                    // NUL bytes indicate a multibyte encoding such as UTF-16/UTF-32.
                    if (buffer[i] == 0) {
                        return false;
                    }
                    char c = (char) (buffer[i] & 0xff);
                    if (c == DOCTYPE_DECLARATION.charAt(doctypeMatched)) {
                        if (++doctypeMatched == DOCTYPE_DECLARATION.length()) {
                            return false;
                        }
                    } else {
                        doctypeMatched = c == DOCTYPE_DECLARATION.charAt(0) ? 1 : 0;
                    }
                }
            }
            return true;
        }
    }

    //XML-related stuff -  resolver+error handler
    private static final EntityResolver ETL_ENTITY_RESOLVER = new EntityResolver() {
        public InputSource resolveEntity(final String publicId, final String systemId) {
            if (systemId != null && systemId.trim().endsWith(DTD_NAME)) {
                InputStream stream = ConfigurationFactory.class.getResourceAsStream(RES_PATH);
                if (stream == null) { //This may happen only in IDE if *.dtd are not copied during compile
                    throw new IllegalStateException("Scriptella required DTD resource " + RES_PATH +
                            " not found on classpath. Please check scriptella.jar and its content.");
                }
                return new InputSource(stream);
            }
            return null;
        }
    };
    private static final ErrorHandler ETL_ERROR_HANDLER = new ErrorHandler() {
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
    };

}
