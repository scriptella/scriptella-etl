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
package scriptella.driver.csv;

import scriptella.AbstractTestCase;
import scriptella.configuration.MockConnectionEl;
import scriptella.configuration.StringResource;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.MockDriverContext;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.util.RepeatingInputStream;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link scriptella.driver.csv.CsvConnection}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class CsvConnectionPerfTest extends AbstractTestCase {
    private int rows;

    private ByteArrayOutputStream out;

    protected void setUp() throws Exception {
        super.setUp();
        testURLHandler = new TestURLHandler() {
            byte[] rows = "c1,c2,c3\nc4,c5,c6\n".getBytes();

            public InputStream getInputStream(final URL u) {
                return new RepeatingInputStream(rows, 10000);
            }

            public OutputStream getOutputStream(final URL u) {
                if (out == null) {
                    out = new ByteArrayOutputStream();
                }
                return out;
            }

            public int getContentLength(final URL u) {
                return -1;
            }
        };

    }

    /**
     * History:
     * 11.09.2006 - Duron 1.7Mhz - 1265 ms
     * 09.09.2006 - Duron 1.7Mhz - 1400 ms
     */
    public void testQuery() {
        //Create a configuration with non default values
        Map<String, String> props = new HashMap<String, String>();
        ConnectionParameters cp = new ConnectionParameters(new MockConnectionEl(props, "tst://file"), MockDriverContext.INSTANCE);

        CsvConnection con = new CsvConnection(cp);
        //Quering 20000 lines file 10 times.
        for (int i=0;i<10;i++) {
            rows = 0;
            con.executeQuery(new StringResource(" ,.*2, "), MockParametersCallbacks.NULL, new QueryCallback() {
                public void processRow(final ParametersCallback parameters) {
                    rows++;
                    parameters.getParameter("c1");
                    parameters.getParameter("2");
                    parameters.getParameter("nosuchcolumn");

                }
            });
            assertEquals(10000-1, rows); //1 line contains headers and not queries by default
            assertNull("No output should be produced by a query", out);
        }


    }

    /**
     * History:
     * 11.09.2006 - Duron 1.7Mhz - 844 ms
     * 09.09.2006 - Duron 1.7Mhz - 875 ms
     * @throws UnsupportedEncodingException
     */
    public void testScript() throws UnsupportedEncodingException {
        //Create a configuration with non default values
        Map<String, String> props = new HashMap<String, String>();
        ConnectionParameters cp = new ConnectionParameters(new MockConnectionEl(props, "tst://file"), MockDriverContext.INSTANCE);

        CsvConnection con = new CsvConnection(cp);
        String expected = "\"*col1*\",\"col2\",\"col3\"\n\"*col21*\",\"col22\",\"col23\"\n";
        for (int i = 0; i < 10000; i++) {
            if (out != null) {
                out.reset();
            }
            con.executeScript(new StringResource("$col1,\"col2\",col3\n${col21},col22,col23"),
                    MockParametersCallbacks.SIMPLE);

        }

        con.close();
        String actual = out.toString(); //checking if script worked correctly
        //out is filled partially because we use out.reset() to minimize memory usage
        //So use indexOf to check
        assertTrue(actual.indexOf(expected) >= 0);
    }


}
