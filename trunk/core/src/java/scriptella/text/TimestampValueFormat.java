package scriptella.text;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Date;

/**
 * Format class to work with dates represented in JDBC escape syntax.
 *
 * @author Fyodor Kupolov
 * @since 1.1
 */
public class TimestampValueFormat extends DateFormat {
    @Override
    public StringBuffer format(Date date, StringBuffer out, FieldPosition fieldPosition) {
        out.append(new Timestamp(date.getTime()).toString());
        return out;
    }

    @Override
    public Date parse(String source, ParsePosition pos) {
        Date result = null;
        result = Timestamp.valueOf(source.substring(pos.getIndex()));
        pos.setIndex(source.length() - 1);
        return result;
    }
}
