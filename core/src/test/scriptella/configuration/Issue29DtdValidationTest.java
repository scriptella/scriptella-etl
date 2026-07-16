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
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URL;

/**
 * Regression test for GitHub issue #29.
 */
public class Issue29DtdValidationTest extends TestCase {
    public void testIncludeInNestedScriptIsValid() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) {
                InputStream dtd = Issue29DtdValidationTest.class
                        .getResourceAsStream("/scriptella/dtd/etl.dtd");
                assertNotNull("Scriptella DTD is missing from the test classpath", dtd);
                return new InputSource(dtd);
            }
        });
        builder.setErrorHandler(new ErrorHandler() {
            public void warning(SAXParseException exception) throws SAXParseException {
                throw exception;
            }

            public void error(SAXParseException exception) throws SAXParseException {
                throw exception;
            }

            public void fatalError(SAXParseException exception) throws SAXParseException {
                throw exception;
            }
        });

        URL issueSample = getClass().getResource("ValidationIncludeTest.xml");
        assertNotNull("Issue #29 validation sample is missing", issueSample);
        builder.parse(issueSample.toString());
    }
}
