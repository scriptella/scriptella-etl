package scriptella.text;

import junit.framework.TestCase;

import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;

/**
 * @author Fyodor Kupolov
 */
public class ValueFormatBuilderTest extends TestCase {
    public static class TestFormat extends Format {

        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return null;
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            return null;
        }
    }

    public void testCustomClass() {
        ValueFormatBuilder b = new ValueFormatBuilder();
        b.setClassName(TestFormat.class.getName());
        Format format = b.build();
        assertTrue(format instanceof TestFormat);
    }

    public void testTimestamp() {
        ValueFormatBuilder b = new ValueFormatBuilder();
        b.setType("timestamp");

        Format format = b.build();
        assertTrue(format instanceof TimestampValueFormat);
    }

    public void testMsgFormat() {
        ValueFormatBuilder b = new ValueFormatBuilder();
        b.setType("number").setPattern("0.0");

        Format format = b.build();
        assertTrue(format instanceof MessageFormat);
    }

}
