package scriptella.text;

import junit.framework.TestCase;
import scriptella.spi.ParametersCallback;
import scriptella.spi.support.MapParametersCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link ColumnFormatter}.
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class ColumnFormatterTest extends TestCase {
    private Map<String, ColumnFormat> formatsMap;

    @Override
    protected void setUp() throws Exception {
        formatsMap = new HashMap<String, ColumnFormat>();
        ColumnFormat cf = new ColumnFormat();
        cf.setNullString("nullStr");
        formatsMap.put("nullStrCol", cf);
        cf = new ColumnFormat();
        cf.setType("number");
        cf.setPattern("00.00");
        formatsMap.put("numberCol", cf);
    }

    public void testParse() {
        ColumnFormatInfo fi = new ColumnFormatInfo(formatsMap, null);
        ColumnFormatter cf = new ColumnFormatter(fi);
        Object result = cf.parse("nullStrCol", "nullStr");
        assertNull(result);
        result = cf.parse("numberCol", "1.1");
        assertEquals(1.1d, ((Number) result).doubleValue(), 0.001);
        result = cf.parse("noSuchCol", "1.1");
        assertEquals("1.1", result);
    }

    public void testParseDefaultNullStr() {
        //Define column format with empty string as null
        ColumnFormatInfo fi = new ColumnFormatInfo(formatsMap, "");
        ColumnFormatter cf = new ColumnFormatter(fi);
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
        ColumnFormatInfo fi = new ColumnFormatInfo(formatsMap, null);
        ColumnFormatter cf = new ColumnFormatter(fi);
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
        ColumnFormatInfo fi = new ColumnFormatInfo(formatsMap, "");
        ColumnFormatter cf = new ColumnFormatter(fi);
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


    public void testFormattingCallback() {
        ColumnFormatInfo fi = new ColumnFormatInfo(formatsMap, null);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("numberCol", 1.1);
        ParametersCallback c = new MapParametersCallback(params);
        ColumnFormatter cf = new ColumnFormatter(fi);
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
        fi = new ColumnFormatInfo(formatsMap, "");
        params.put("numberCol", 1);
        cf = new ColumnFormatter(fi);
        formatter = cf.format(c);
        result = formatter.getParameter("numberCol");
        assertEquals("Numeric value must be formatted", "01.00", result);
        result = formatter.getParameter("nullStrCol");
        assertEquals("null must be formatted as nullStr", "nullStr", result);
        result = formatter.getParameter("nosuchcol");
        assertEquals("Empty string must be returned for nonexistent column", "", result);
    }


}
