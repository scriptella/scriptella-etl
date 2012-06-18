package scriptella.text;

import junit.framework.TestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Tests for {@link ColumnFormat}
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class ColumnFormatTest extends TestCase {

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyy", Locale.US);

    public void testSetProperty() {
        ColumnFormat ci = new ColumnFormat();
        ci.setProperty("type", "number");
        ci.setProperty("pattern", "#");
        ci.setProperty("locale", "ru_RU");
        ci.setProperty("trim", "true");
        ci.setProperty("null_string", "NS");
        assertEquals("NS", ci.getNullString());
        final Locale locale = ci.getLocale();
        assertEquals("RU", locale.getCountry());
        assertEquals("ru", locale.getLanguage());
        assertTrue(ci.isTrim());
        assertEquals("#", ci.getPattern());
        assertEquals("1", ci.getFormat().format(new Object[]{1.1}));
    }

    public void testNumberFormat() {
        ColumnFormat ci = new ColumnFormat();
        ci.setType("number");
        ci.setPattern("00.00");
        ci.setLocaleStr("en_US");
        assertEquals("01.10", ci.format(1.1));
        //If array is passed, only the first value is used.
        assertEquals("01.10", ci.format(new Object[]{1.1, 1.2}));
        assertEquals(1.23, (Double) ci.parse("1.23"), 0.0001);
        assertNull(ci.parse(null));
        assertNull(ci.format(null));

        ci.setNullString("ns");
        assertEquals("ns", ci.format(null));
        assertNull(ci.parse("ns"));

    }

    public void testDateFormat() throws ParseException {
        ColumnFormat ci = new ColumnFormat();
        ci.setType("date");
        ci.setPattern("ddMMyy");
        ci.setLocaleStr("en_US");

        Date d = simpleDateFormat.parse("01012012");
        assertEquals("010112", ci.format(d));
        assertEquals(d, ci.parse("010112"));

        //wrong data
        try {
            ci.parse("----");
            fail("An error should be thrown for wrong format");
        } catch (IllegalArgumentException e) {
            //OK
        }

        //trim
        try {
            ci.parse(" \n010112 ");
            fail("An error should be thrown for wrong format");
        } catch (IllegalArgumentException e) {
            //OK
        }
        ci.setTrim(true);
        assertEquals("Value must be trimmed before parsing", d, ci.parse(" \n010112 "));
    }

}
