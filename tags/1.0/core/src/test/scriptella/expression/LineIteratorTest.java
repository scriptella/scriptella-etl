/*
 * Copyright 2006-2009 The Scriptella Project Team.
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
package scriptella.expression;

import scriptella.AbstractTestCase;
import scriptella.core.RuntimeIOException;
import scriptella.spi.MockParametersCallbacks;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.NoSuchElementException;

/**
 * Tests for {@link scriptella.expression.LineIterator}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class LineIteratorTest extends AbstractTestCase {
    public void testMinimal() {
        LineIterator it = new LineIterator(new StringReader(
                " L1\n L2 \r\nL3 "
        ));
        assertTrue(it.hasNext());
        assertEquals(" L1", it.next());
        assertEquals(" L2 ", it.next());
        assertEquals("L3 ", it.next());
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException e) {
            //OK
        }
    }

    public void test() throws IOException {
        PropertiesSubstitutor ps = new PropertiesSubstitutor(MockParametersCallbacks.SIMPLE);
        LineIterator it = new LineIterator(new StringReader(
                " test\n-$prop  \r  \n   ${ p3}  "
        ), ps, true);
        assertTrue(it.hasNext());
        assertEquals("test", it.next());
        assertEquals("-*prop*", it.next());
        assertTrue(it.hasNext());
        //Also test reentrance
        assertTrue(it.hasNext());
        assertEquals("", it.next());
        assertEquals("*p3*", it.next());
        assertFalse(it.hasNext());
        //Reentrance test for EOF
        assertFalse(it.hasNext());

        //Test illegal close
        it.close();
        it.close();
        assertFalse(it.hasNext());
    }

    /**
     * Tests for illegal arguments
     */
    public void testIllegalUsage() {
        try {
            new LineIterator(null);
            fail("Reader must be checked for null");
        } catch (IllegalArgumentException e) {
            //OK
        }
        try {
            new LineIterator(new StringReader("")).remove();
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {
            //OK
        }
    }

    /**
     * Tests if IO exceptions are correctly handled and propagated.
     */
    public void testIOExceptions() {
        final IOException ioe1 = new IOException("Unable to read");
        final IOException ioe2 = new IOException("Unable to close");
        Reader r = new Reader("test") {
            public int read(char cbuf[], int off, int len) throws IOException {
                throw ioe1;
            }

            public void close() throws IOException {
                throw ioe2;
            }

        };
        LineIterator it = new LineIterator(r,
                new PropertiesSubstitutor(MockParametersCallbacks.NULL));
        try {
            it.hasNext();
            fail(ioe1 + " was expected");
        } catch (RuntimeIOException e) {
            assertEquals(ioe1, e.getCause());
            //OK
        }
        try {
            it.close();
            fail(ioe2 + " was expected");
        } catch (IOException e) {
            assertEquals(ioe2, e);
        }
    }

    /**
     * Tests if skip works
     */
    public void testSkip() {
        LineIterator lit = new LineIterator(new StringReader("a\nb\nc\nd"));
        lit.skip(0);
        assertEquals("a", lit.next());
        assertEquals(2, lit.skip(2));
        assertEquals("d", lit.next());
        assertEquals(0, lit.skip(1)); //move outside
        assertFalse(lit.hasNext());
        lit.skip(-3); //nagatives are ingored
        assertFalse(lit.hasNext());
    }

    public void testLineAt() {
        LineIterator it = new LineIterator(new StringReader(
                " L1\nL2 \r\nL3 "
        ));
        assertEquals("L2 ", it.getLineAt(1));
        assertEquals(null, it.getLineAt(1));
    }
}
