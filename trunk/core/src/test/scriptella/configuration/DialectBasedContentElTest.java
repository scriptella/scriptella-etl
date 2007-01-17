/*
 * Copyright 2006-2007 The Scriptella Project Team.
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

import scriptella.AbstractTestCase;
import scriptella.spi.DialectIdentifier;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Tests for {@link DialectBasedContentEl} class.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DialectBasedContentElTest extends AbstractTestCase {

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
        XmlElement xmlElement = XmlElementTest.asElement(xml);
        DialectBasedContentEl d = new DialectBasedContentEl(xmlElement);
        List<DialectBasedContentEl.Dialect> dialects = d.getDialects();
        assertEquals(5, dialects.size());
        DialectBasedContentEl.Dialect dialect = dialects.get(0);
        assertEquals(" \nDef1Def2\n", asString(dialect.getContentEl()));
        dialect = dialects.get(1);
        assertEquals("D1", asString(dialect.getContentEl()));
        assertTrue(dialect.getName().matcher("d1").matches());
        dialect = dialects.get(2);
        assertEquals("Def3", asString(dialect.getContentEl()));
        assertNull(dialect.getName());
        dialect = dialects.get(3);
        assertEquals("D2", asString(dialect.getContentEl()));
        assertTrue(dialect.getName().matcher("d2").matches());
        dialect = dialects.get(4);
        assertEquals("\nDef4  ", asString(dialect.getContentEl()));
        assertNull(dialect.getName());

        //Now check if dialects are searched correctly
        assertEquals(" \nDef1Def2\nDef3\nDef4  ", asString(d.getContent(null)));
        assertEquals(" \nDef1Def2\nDef3\nDef4  ", asString(d.getContent(new DialectIdentifier("XDialect", "0.0"))));
        assertEquals(" \nDef1Def2\nD1Def3\nDef4  ", asString(d.getContent(new DialectIdentifier("d1", "1.0"))));
        assertEquals(" \nDef1Def2\nDef3D2\nDef4  ", asString(d.getContent(new DialectIdentifier("d2", "1.0"))));
    }

    /**
     * Tests {@link scriptella.configuration.DialectBasedContentEl.Dialect} class.
     */
    public void testDialect() {
        DialectBasedContentEl.Dialect d = new DialectBasedContentEl.Dialect();
        //null dialect should be matched
        assertTrue(d.matches(null));
        assertTrue(d.matches(DialectIdentifier.NULL_DIALECT));
        d.setName(Pattern.compile(".*"));
        assertTrue(d.matches(DialectIdentifier.NULL_DIALECT));
        d.setName(Pattern.compile("dl"));
        assertFalse(d.matches(DialectIdentifier.NULL_DIALECT));
    }

}
