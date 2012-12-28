package scriptella.text;

import junit.framework.TestCase;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Tests for {@link PropertyFormat}
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class PropertyFormatTest extends TestCase {

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyy", Locale.US);

    public void testNumberFormat() {
        PropertyFormat ci = new PropertyFormat();
        ci.setType("number");
        ci.setPattern("00.00");
        ci.setLocale(Locale.US);
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

    public void testChoiceFormat() {
        PropertyFormat ci = new PropertyFormat();
        ci.setType("choice");
        ci.setPattern("0#no files|1#{0} file|2<{0} files");
        ci.setLocale(Locale.US);
        assertEquals("no files", ci.format(0));
        assertEquals("1 file", ci.format(1));
        assertEquals("3 files", ci.format(3));
        //FIXME Parse not supported for now
    }

    public void testDateFormat() throws ParseException {
        PropertyFormat ci = new PropertyFormat();
        ci.setType("date");
        ci.setPattern("ddMMyy");
        ci.setLocale(Locale.US);

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

    public void testTimestampFormat() throws ParseException {
        PropertyFormat ci = new PropertyFormat();
        ci.setType("timestamp");

        String expectedStr = "2012-05-25 01:02:03.0";
        Date expectedTs = Timestamp.valueOf(expectedStr);
        assertEquals(expectedStr, ci.format(expectedTs));
        assertEquals(expectedTs, ci.parse(expectedStr));

        //wrong data
        try {
            ci.parse("----");
            fail("An error should be thrown for wrong format");
        } catch (IllegalArgumentException e) {
            //OK
        }

        assertEquals("Value with spaces must be parsed", expectedTs, ci.parse(" \n"+expectedStr));
    }

    public void testPad() throws ParseException {
        PropertyFormat ci = new PropertyFormat();
        ci.setType("number");
        ci.setPattern("0.0");
        ci.setPadLeft(5);

        String expectedStr = "  1.0";
        String actualStr = ci.format(1);
        assertEquals(expectedStr, actualStr);
    }



}
