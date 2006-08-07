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

import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import scriptella.AbstractTestCase;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.StringReader;
import java.net.URL;
import java.util.List;

/**
 * Tests for {@link DialectBasedContentEl} class.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DialectBasedContentElTest extends AbstractTestCase {
    private static final DocumentBuilder BUILDER;

    static {
        try {
            BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }


    /**
     * Tests handling of mixed dialogs content.
     */
    public void test() {
        String xml = "<query>" +
                " \nDef1<!--Comment -->De<!--Comment2-->f2\n" +
                "<dialect name=\"d1\">D<!--Comment-->1</dialect>" +
                "<![CDATA[De]]>f3" +
                "<dialect name=\"d2\"><!--Commend-->D2</dialect>" +
                "<!--Comment-->\nDef4<!--Comment-->  " +
                "</query>";
        XMLElement xmlElement = asElement(xml);
        DialectBasedContentEl d = new DialectBasedContentEl(xmlElement);
        List<DialectBasedContentEl.Dialect> dialects = d.getDialects();
        assertEquals(3, dialects.size());
        DialectBasedContentEl.Dialect dialect = dialects.get(0);
        assertEquals("D1", asString(dialect.getContentEl()));
        dialect = dialects.get(1);
        assertEquals("D2", asString(dialect.getContentEl()));
        dialect = dialects.get(2);
        assertEquals(" \nDef1Def2\nDef3\nDef4  ", asString(dialect.getContentEl()));

    }

    private static XMLElement asElement(String xml) {
        try {
            Element el = BUILDER.parse(new InputSource(new StringReader(xml))).getDocumentElement();
            return new XMLElement(el, new URL("file:/test"));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create XML element", e);
        }
    }
}
