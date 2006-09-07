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
package scriptella.driver.velocity;

import scriptella.AbstractTestCase;
import scriptella.configuration.StringResource;
import scriptella.spi.MockConnectionParameters;
import scriptella.spi.MockParametersCallbacks;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Tests velocity connection class.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class VelocityConnectionTest extends AbstractTestCase {

    /**
     * This test creates a velocity connection that produces output into memory.
     * Context chaining is also tested.
     */
    public void test() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        VelocityConnection c = createConnection(out);
        run(c);
        c.close();
        String s = out.toString();
        assertEquals("*v1*///*v2*", s);


    }
    //Methods shared with performance test

    static VelocityConnection createConnection(final OutputStream out) {
        try {
            URL u = new URL("mem", "", 0, "memfile", new URLStreamHandler() {
                protected URLConnection openConnection(URL u) {
                    return new URLConnection(u) {
                        public void connect() {
                        }

                        public InputStream getInputStream() {
                            throw new UnsupportedOperationException();
                        }

                        public OutputStream getOutputStream() {
                            return out;
                        }
                    };
                }
            });
            return new VelocityConnection(u, null, MockConnectionParameters.NULL);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    static void run(VelocityConnection c) {
        c.executeScript(new StringResource(("$v1///$v2")), MockParametersCallbacks.SIMPLE);
    }
}
