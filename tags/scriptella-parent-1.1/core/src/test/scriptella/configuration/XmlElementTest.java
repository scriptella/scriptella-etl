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

import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import scriptella.AbstractTestCase;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.MockParametersCallbacks;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.StringReader;
import java.net.URL;

/**
 * Tests for {@link XmlElement}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class XmlElementTest extends AbstractTestCase {
    private static final DocumentBuilder BUILDER;

    static {
        try {
            BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void testGetXPath() {
        String xml = "<etl>\n" +
                "     <query connection-id=\"db1\">\n" +
                "        query1" +
                "        <script connection-id=\"db2\">\n" +
                "            SCRIPT1" +
                "        </script>\n" +
                "    </query>\n" +
                "    <query connection-id=\"db3\">\n" +
                "       query2" +
                "        <script connection-id=\"db4\">\n" +
                "            script2" +
                "        </script>\n" +
                "    </query>\n" +
                "\n" +
                "</etl>";
        XmlElement root = asElement(xml);
        //selecting second query, first script
        XmlElement el = root.getChildren("query").get(1).getChild("script");
        assertEquals("/etl/query[2]/script[1]", el.getXPath());

    }

    public void testGetXPathById() {
        String xml = "<etl>\n" +
                "    <query connection-id=\"db1\">\n" +
                "        query1\n" +
                "        <script connection-id=\"db2\">\n" +
                "            SCRIPT1\n" +
                "        </script>\n" +
                "    </query>\n" +
                "    <query connection-id=\"db3\" id=\"query2\">\n" +
                "        query2\n" +
                "        <query connection-id=\"db1\" id=\"query3\">\n" +
                "            query3\n" +
                "            <script connection-id=\"db4\">\n" +
                "                script2\n" +
                "            </script>\n" +
                "        </query>\n" +
                "    </query>\n" +
                "\n" +
                "</etl>";
        XmlElement root = asElement(xml);
        //selecting second query, first script
        XmlElement el = root.getChildren("query").get(1).getChild("query").getChild("script");
        assertEquals("id(\"query3\")/script[1]", el.getXPath());

    }

    static XmlElement asElement(String xml) {
        try {
            Element el = BUILDER.parse(new InputSource(new StringReader(xml))).getDocumentElement();
            return new XmlElement(el, new URL("file:/test"), new PropertiesSubstitutor(MockParametersCallbacks.NULL));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create XML element", e);
        }
    }

}
