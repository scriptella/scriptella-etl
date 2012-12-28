package scriptella.text;

import junit.framework.TestCase;

import java.sql.Timestamp;
import java.util.Date;

/**
 * @author Fyodor Kupolov
 */
public class TimestampValueFormatTest extends TestCase {
    public void testFormat() throws Exception {
        String dateStr = "2012-5-25 01:02:03";
        String expectedStr = "2012-05-25 01:02:03.0";
        Date date = Timestamp.valueOf(dateStr);
        String actualStr = new TimestampValueFormat().format(date);
        assertEquals(expectedStr, actualStr);
    }

    public void testParse() throws Exception {
        String dateStr = "2012-5-25 01:02:03";
        Date expectedDate = Timestamp.valueOf(dateStr);
        Date actualDate = new TimestampValueFormat().parse(dateStr);
        assertEquals(expectedDate, actualDate);

        //test malformed  values
        try {
            new TimestampValueFormat().parse("");
        } catch (IllegalArgumentException e) {
            System.out.println("Expected " + e);
        }
    }
}
