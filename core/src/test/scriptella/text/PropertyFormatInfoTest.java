package scriptella.text;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link PropertyFormatInfo}.
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class PropertyFormatInfoTest extends TestCase {

    public void testParse() throws Exception {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("format.colA.pattern", "00.0");
        map.put("format.colA.type", "number");
        map.put("format.col.B.trim", "true"); //dots in names are allowed
        map.put("format.col.B.null_string", "");
        map.put("parse.colC.null_string", ""); //should be ignored
        final PropertyFormatInfo metadata = PropertyFormatInfo.parse(new TypedPropertiesSource(map), "format.");
        final Set<String> names = metadata.getFormatMap().keySet();
        assertEquals(new LinkedHashSet<String>(Arrays.asList("colA", "col.B")), names);
        PropertyFormat col = metadata.getPropertyFormat("colA");
        assertEquals("00.0", col.getPattern());
        assertEquals("number", col.getType());
        col = metadata.getPropertyFormat("col.B");
        assertEquals("", col.getNullString());
        assertTrue(col.isTrim());
    }

    public void testParseLegacyNullString() throws Exception {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("format.colA.pattern", "00.0");
        map.put("format.colA.type", "number");
        map.put("format.col.B.trim", "true"); //dots in names are allowed
        map.put("format.col.B.null_string", "");
        map.put("parse.colC.null_string", ""); //should be ignored
        final PropertyFormatInfo metadata = PropertyFormatInfo.parse(new TypedPropertiesSource(map), "format.");
        final Set<String> names = metadata.getFormatMap().keySet();
        assertEquals(new LinkedHashSet<String>(Arrays.asList("colA", "col.B")), names);
        PropertyFormat col = metadata.getPropertyFormat("colA");
        assertEquals("00.0", col.getPattern());
        assertEquals("number", col.getType());
        col = metadata.getPropertyFormat("col.B");
        assertEquals("", col.getNullString());
        assertTrue(col.isTrim());
    }


    public void testParseNoPrefix() throws Exception {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("colA.pattern", "00.0");
        map.put("colB.trim", "true");
        map.put("prefix.column.type", "number"); //wrong definition should be recognized as "prefix"
        final PropertyFormatInfo metadata = PropertyFormatInfo.parse(new TypedPropertiesSource(map), "");
        final Set<String> names = metadata.getFormatMap().keySet();
        assertEquals(new LinkedHashSet<String>(Arrays.asList("colA", "colB", "prefix.column")), names);
    }

    public void testSetProperty() {
        PropertyFormat ci = new PropertyFormat();
        Map<String, Object> p = new HashMap<String, Object>();
        p.put("a.type", "number");
        p.put("a.pattern", "#");
        p.put("a.locale", "ru_RU");
        p.put("a.trim", "true");
        p.put("a.null_string", "NS");
        TypedPropertiesSource ps = new TypedPropertiesSource(p);

        PropertyFormatInfo.setProperty(ci, "type", "a.type", ps);
        PropertyFormatInfo.setProperty(ci, "pattern", "a.pattern", ps);
        PropertyFormatInfo.setProperty(ci, "locale", "a.locale", ps);
        PropertyFormatInfo.setProperty(ci, "trim", "a.trim", ps);
        PropertyFormatInfo.setProperty(ci, "null_string", "a.null_string", ps);
        assertEquals("NS", ci.getNullString());
        final Locale locale = ci.getLocale();
        assertEquals("RU", locale.getCountry());
        assertEquals("ru", locale.getLanguage());
        assertTrue(ci.isTrim());
        assertEquals("#", ci.getPattern());
        assertEquals("1", ci.getFormat().format(new Object[]{1.1}));
    }

    public void testDefaults() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("format.trim", "true");
        map.put("format.null_string", "--");
        map.put("format.pad_left", "10");
        map.put("format.pad_right", "20");
        map.put("format.pad_char", "_");
        map.put("format.testProp.type", "number");
        map.put("format.testProp.trim", "false");
        final PropertyFormatInfo metadata = PropertyFormatInfo.parse(new TypedPropertiesSource(map), "format.");
        assertTrue(metadata.getDefaultFormat().isTrim());
        assertEquals("--", metadata.getDefaultFormat().getNullString());
        assertEquals(10, metadata.getDefaultFormat().getPadLeft());
        assertEquals(20, metadata.getDefaultFormat().getPadRight());
        assertEquals('_', metadata.getDefaultFormat().getPadChar());
        final PropertyFormat p = metadata.getPropertyFormat("testProp");
        assertEquals("number", p.getType());
        assertEquals(10, p.getPadLeft());
        assertEquals(20, p.getPadRight());
        assertEquals('_', p.getPadChar());
        assertEquals('_', p.getPadChar());
        assertFalse(p.isTrim());
    }

}
