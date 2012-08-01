/*
 * Copyright 2006-2012 The Scriptella Project Team.
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

package scriptella.driver.text;

import scriptella.DBTestCase;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Tests CSV column formatting.
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class TextFormattingITest extends DBTestCase {
    public void test() throws EtlExecutorException {
        getConnection("text");//Call just to close the DB
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                return new ByteArrayInputStream(("10,Ten,Ten,12-07-2012 10:00,10.1\n" +
                        "11, Eleven , Eleven , 12-07-2012 11:00,-null-").getBytes());
            }

            public OutputStream getOutputStream(final URL u) {
                return out;
            }

            public int getContentLength(final URL u) {
                throw new UnsupportedOperationException();
            }
        };
        final EtlExecutor se = newEtlExecutor();
        se.execute();
        //10,11 where parsed from CSV, 1,2,3 were directly inserted into DB
        String expectedResult = "1/One/One/11-07-2012 22:33/-null-\n" +
                "2/Two/ Two /11-07-2012 20:00/2.10\n" +
                "3/Three/ Three /11-07-2012 20:00/3.10\n" +
                "10/Ten/Ten/12-07-2012 10:00/10.10\n" +
                "11/Eleven/ Eleven /12-07-2012 11:00/-null-\n";
        assertEquals(expectedResult, out.toString());
    }
}
