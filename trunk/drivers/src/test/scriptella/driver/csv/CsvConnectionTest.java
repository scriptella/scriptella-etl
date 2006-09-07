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

import scriptella.AbstractTestCase;
import scriptella.configuration.StringResource;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.MockDriversContext;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link CsvConnection}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class CsvConnectionTest extends AbstractTestCase {
    private int rows;

    public void testConfigure() {

        //Create a configuration with non default values
        Map<String, String> props = new HashMap<String, String>();
        props.put(CsvConnection.ENCODING, "UTF8");
        props.put(CsvConnection.EOL, "\r\n");
        props.put(CsvConnection.HEADERS, "false");
        props.put(CsvConnection.QUOTE, "'");
        props.put(CsvConnection.SEPARATOR, ";");
        ConnectionParameters cp = new ConnectionParameters(props, "tst://file", null, null, null, null,
                MockDriversContext.INSTANCE);

        CsvConnection con = new CsvConnection(cp);
        //register handler for tst url
        testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                try {
                    return new ByteArrayInputStream("c1;c2;c3\nc4;'c''5';c6\u0394".getBytes("UTF8"));
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }

            public OutputStream getOutputStream(final URL u) {
                throw new UnsupportedOperationException();
            }

            public int getContentLength(final URL u) {
                return -1;
            }
        };
        rows = 0;
        con.executeQuery(new StringResource(""), MockParametersCallbacks.UNSUPPORTED, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                rows++;
                switch (rows) {
                    case 1:
                        assertEquals("c1", parameters.getParameter("1"));
                        assertEquals("c2", parameters.getParameter("2"));
                        assertEquals("c3", parameters.getParameter("3"));
                        break;
                    case 2:
                        assertEquals("c4", parameters.getParameter("1"));
                        assertEquals("c'5", parameters.getParameter("2"));
                        assertEquals("c6\u0394", parameters.getParameter("3"));
                        break;

                    default:
                        fail("unexpected row " + rows);
                }

            }
        });
        assertEquals(2, rows);


    }

}
