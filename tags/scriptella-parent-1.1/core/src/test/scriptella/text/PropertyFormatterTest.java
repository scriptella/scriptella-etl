package scriptella.text;

import junit.framework.TestCase;
import scriptella.spi.ParametersCallback;
import scriptella.spi.support.MapParametersCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link PropertyFormatter}.
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class PropertyFormatterTest extends TestCase {
    private TypedPropertiesSource props;
    private Map<String, String> formatsMap;

    @Override
    protected void setUp() throws Exception {
        formatsMap = new HashMap<String, String>();
        formatsMap.put("nullStrCol.null_string", "nullStr");
        formatsMap.put("numbercol.type", "number");
        formatsMap.put("numbercol.pattern", "00.00");
        formatsMap.put("padNumbercol.type", "number");
        formatsMap.put("padNumbercol.pattern", "0.0");
        formatsMap.put("padNumbercol.pad_right", "5");
        formatsMap.put("padNumbercol2.type", "number");
        formatsMap.put("padNumbercol2.pattern", "0.0");
        formatsMap.put("padNumbercol2.pad_left", "5");
        formatsMap.put("padNumbercol2.pad_char", "_");

        props = new TypedPropertiesSource(formatsMap);
    }

    public void testParse() {
        PropertyFormatInfo fi = PropertyFormatInfo.parse(props, "");
        PropertyFormatter pf = new PropertyFormatter(fi);
        Object result = pf.parse("nullStrCol", "nullStr");
        assertNull(result);
        result = pf.parse("numberCol", "1.1");
        assertEquals(1.1d, ((Number) result).doubleValue(), 0.001);
        result = pf.parse("noSuchCol", "1.1");
        assertEquals("1.1", result);
        //Now test parsing of an integer with spaces
        result = pf.parse("numberCol", " 1.1 ");
        assertEquals(1.1d, ((Number) result).doubleValue(), 0.001);
    }

    public void testParseDefaultNullStr() {
        //Define column format with empty string as null
        formatsMap.put("null_string", "");
        PropertyFormatInfo fi = PropertyFormatInfo.parse(props, "");
        PropertyFormatter cf = new PropertyFormatter(fi);
        Object result = cf.parse("nullStrCol", "nullStr");
        assertNull(result);
        result = cf.parse("nullStrCol", "");
        assertEquals("Column specific null_string must be used", "", result);
        result = cf.parse("numberCol", "1.1");
        assertEquals(1.1d, ((Number) result).doubleValue(), 0.001);
        result = cf.parse("numberCol", "");
        assertNull("Default null_string must be used if column format does not specify one", result);
        result = cf.parse("numberCol", null);
        assertNull("Null string must be parsed as null for a number column", result);
        result = cf.parse("noSuchCol", "");
        assertNull("Default null_string must be used when column does not have a format", result);
        result = cf.parse("noSuchCol", "1.1");
        assertEquals("1.1", result);
    }

    public void testFormat() {

        PropertyFormatInfo fi = PropertyFormatInfo.parse(props, "");
        PropertyFormatter cf = new PropertyFormatter(fi);
        Object result = cf.format("nullStrCol", "");
        assertEquals("Unmodified value is expected", "", result);
        result = cf.format("nullStrCol", null);
        assertEquals("null must be formatted as nullStr", "nullStr", result);
        result = cf.format("numberCol", 1.1);
        assertEquals("Numeric value must be formatted", "01.10", result);
        result = cf.format("noSuchCol", 1.1);
        assertEquals("Default toString conversion is expected", "1.1", result);
    }

    public void testFormatDefaultNullStr() {
        formatsMap.put("null_string", "");

        PropertyFormatInfo fi = PropertyFormatInfo.parse(props, "");
        PropertyFormatter cf = new PropertyFormatter(fi);
        Object result = cf.format("nullStrCol", "");
        assertEquals("Unmodified value is expected", "", result);
        result = cf.format("nullStrCol", null);
        assertEquals("null must be formatted as nullStr", "nullStr", result);
        result = cf.format("numberCol", 1.1);
        assertEquals("Numeric value must be formatted", "01.10", result);
        result = cf.format("numberCol", null);
        assertEquals("Empty string must be returned for null", "", result);

        result = cf.format("noSuchCol", 1.1);
        assertEquals("Default toString conversion is expected", "1.1", result);
    }

    public void testPadding() {
        //Set default padding options
        formatsMap.put("pad_left", "8");
        formatsMap.put("pad_char", "-");
        PropertyFormatInfo fi = PropertyFormatInfo.parse(props, "");
        PropertyFormatter pf = new PropertyFormatter(fi);
        String result = pf.format("padNumberCol", 1);
        assertEquals("-----1.0", result);
        result = pf.format("padNumberCol2", 1);
        assertEquals("__1.0", result);
        //Test default padding
        result = pf.format("NoSuchCol", 1);
        assertEquals("-------1", result);

    }

    public void testFormattingCallback() {
        PropertyFormatInfo fi = PropertyFormatInfo.parse(props, "");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("numberCol", 1.1);
        ParametersCallback c = new MapParametersCallback(params);
        PropertyFormatter cf = new PropertyFormatter(fi);
        ParametersCallback formatter = cf.format(c);
        Object result = formatter.getParameter("numberCol");
        assertEquals("Numeric value must be formatted", "01.10", result);
        result = formatter.getParameter("nullStrCol");
        assertEquals("null must be formatted as nullStr", "nullStr", result);
        result = formatter.getParameter("nosuchcol");
        assertNull("Null must be returned for nonexistent column", result);

        //Now test a case,when number column is null
        params.clear();
        result = formatter.getParameter("numberCol");
        assertNull("Null must be returned for numeric column without a value", result);

        //Now test with empty string as null string
        formatsMap.put("null_string", "");
        fi = PropertyFormatInfo.parse(props, "");
        params.put("numberCol", 1);
        cf = new PropertyFormatter(fi);
        formatter = cf.format(c);
        result = formatter.getParameter("numberCol");
        assertEquals("Numeric value must be formatted", "01.00", result);
        result = formatter.getParameter("nullStrCol");
        assertEquals("null must be formatted as nullStr", "nullStr", result);
        result = formatter.getParameter("nosuchcol");
        assertEquals("Empty string must be returned for nonexistent column", "", result);
    }

    public void testDefaults() {
        formatsMap.put("pad_left", "10");
        formatsMap.put("pad_char", "_");
        PropertyFormatInfo fi = PropertyFormatInfo.parse(props, "");
        PropertyFormatter pf = new PropertyFormatter(fi);
        final String s = pf.format("numbercol", 2);
        assertEquals("_____02.00", s);
    }


}
