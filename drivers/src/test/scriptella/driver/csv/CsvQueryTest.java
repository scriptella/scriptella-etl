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
package scriptella.driver.csv;

import au.com.bytecode.opencsv.CSVReader;
import scriptella.AbstractTestCase;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.io.IOException;
import java.io.StringReader;

/**
 * Tests for {@link CsvQuery}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class CsvQueryTest extends AbstractTestCase {
    private int rows;
    public void test() throws IOException {
        //Test query with columns number exceeding the input data columns number
        String data = "1,2,3\n11,2,3,4\n1";
        String query = ".*1.*,2,3,4";
        CsvQuery q = new CsvQuery(new CSVReader(new StringReader(data)), false, false);
        rows=0;
        //only 11,2,3,4 matches the pattern
        q.execute(new CSVReader(new StringReader(query)), MockParametersCallbacks.UNSUPPORTED, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                rows++;
                assertEquals("11", parameters.getParameter("1"));
                assertEquals("2", parameters.getParameter("2"));
                assertEquals("3", parameters.getParameter("3"));
                assertEquals("4", parameters.getParameter("4"));
            }
        });
        assertEquals(1, rows);
    }

    /**
     * Tests if query correctly works with parameters.
     * @throws IOException
     */
    public void testParametersLookup() throws IOException {
        String data = "a,b,c\n11,22,33";
        String query = "11,22,33";
        CsvQuery q = new CsvQuery(new CSVReader(new StringReader(data)), true, true);
        rows=0;
        q.execute(new CSVReader(new StringReader(query)), MockParametersCallbacks.SIMPLE, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                rows++;
                //now lookup columns by name and index
                assertEquals("11", parameters.getParameter("1"));
                assertEquals("11", parameters.getParameter("a"));
                assertEquals("22", parameters.getParameter("2"));
                assertEquals("22", parameters.getParameter("b"));
                assertEquals("33", parameters.getParameter("3"));
                assertEquals("33", parameters.getParameter("c"));
                assertEquals("*4*", parameters.getParameter("4")); //Unknown column
                assertEquals("*four*", parameters.getParameter("four")); //Unknown column
            }
        });
        assertEquals(1, rows);
    }

    /**
     * Tests if invalid queries are recognized.
     */
    public void testInvalidQuery() throws IOException {
        String data = "a,b,c";
        String query = "\\"; //bad query
        CsvQuery q = new CsvQuery(new CSVReader(new StringReader(data)), true, true);
        try {
            q.execute(new CSVReader(new StringReader(query)), MockParametersCallbacks.UNSUPPORTED, null );
            fail("Bad query syntax should be recognized");
        } catch (CsvProviderException e) {
            //OK
        }


    }

}
