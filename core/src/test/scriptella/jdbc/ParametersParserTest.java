/*
 * Copyright 2006 The Scriptella Project Team.
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
package scriptella.jdbc;

import junit.framework.TestCase;
import scriptella.expression.Expression;
import scriptella.spi.MockDriverContext;
import scriptella.spi.MockParametersCallbacks;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Tests for {@link ParametersParser}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ParametersParserTest extends TestCase {
    /**
     * Tests valid file references
     */
    public void testValid() throws MalformedURLException {

        final MockDriverContext dc = new MockDriverContext();
        ParametersParser p = new ParametersParser(dc);

        String expr = "file 'myfile'";
        Lobs.UrlBlob actual = (Lobs.UrlBlob) p.evaluate(expr, MockParametersCallbacks.UNSUPPORTED);
        URL expected = dc.resolve("myfile");
        assertEquals(expected, actual.getUrl());
        expr = "file 'http'+'://host/'+name";
        actual = (Lobs.UrlBlob) p.evaluate(expr, MockParametersCallbacks.SIMPLE);
        expected = new URL("http://host/*name*");
        assertEquals(expected, actual.getUrl());
    }

    /**
     * Tests invalid file references
     */
    public void testInvalid() throws MalformedURLException {

        final MockDriverContext dc = new MockDriverContext();
        ParametersParser p = new ParametersParser(dc);

        try {
            p.evaluate("file myfile'", MockParametersCallbacks.UNSUPPORTED);
            fail("illegal file reference");
        } catch (Expression.ParseException e) {
            //ok
        }
        //not a file reference, just an expression starting with file prefix
        try {
            p.evaluate("file + 'text'", MockParametersCallbacks.NULL);
        } catch (Expression.EvaluationException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
        }
        //not a file reference, just an expression starting with file prefix
        final Object o = p.evaluate("file + var", MockParametersCallbacks.SIMPLE);
        assertEquals("*file**var*",o);






    }

}
