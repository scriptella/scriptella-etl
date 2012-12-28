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
package scriptella.driver.script;

import junit.framework.Assert;
import scriptella.AbstractTestCase;
import scriptella.configuration.ConfigurationException;
import scriptella.configuration.StringResource;
import scriptella.spi.Connection;
import scriptella.spi.IndexedQueryCallback;
import scriptella.spi.MockConnectionParameters;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;
import scriptella.spi.Resource;

import javax.script.ScriptException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;

/**
 * Tests for {@link ScriptConnection}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 * @since 11.01.2007
 */
public class ScriptConnectionTest extends AbstractTestCase {
    private Object v;

    public void setValue(Object v) {
        this.v = v;
    }

    public void testExecuteScript() {
        v = null;
        Resource r = new StringResource("for (var x=0;x<=10;x+=2) {obj.setValue(x);}");

        newConnection().executeScript(r, MockParametersCallbacks.fromMap(Collections.singletonMap("obj", this)));
        assertEquals(10, ((Number) v).intValue());
        //Now test invalid syntax
        r = new StringResource("nosuchvar.nosuchmethod()");
        try {
            newConnection().executeScript(r, MockParametersCallbacks.NULL);
            fail("Compilation errors must be reported");
        } catch (ScriptProviderException e) {
            Assert.assertTrue("ScriptException is expected to be the cause, but was " + e.getCause(),
                    e.getCause() instanceof ScriptException);
            //OK
        }
    }

    static ScriptConnection newConnection() {
        return new ScriptConnection(new MockConnectionParameters());
    }

    /**
     * Tests various configuration options.
     */
    public void testConfiguration() {
        //JavaScript(ECMAScript) should be used by default
        Connection c = new Driver().connect(new MockConnectionParameters());
        assertEquals("ECMAScript", c.getDialectIdentifier().getName());
        //now tests wrong name
        try {
            new Driver().connect(new MockConnectionParameters(
                    Collections.singletonMap(ScriptConnection.LANGUAGE, "nusuchlanguage"), null));
            fail("ConfigurationException expected for unknown language");
        } catch (ConfigurationException e) {
            //OK
        }
    }

    public void testExecuteQuery() {
        Resource r = new StringResource("i=0;a=a0;s='test';while (i < 10) {i=i+1;query.next();}");
        IndexedQueryCallback callback = new IndexedQueryCallback() {
            protected void processRow(final ParametersCallback parameters, final int rowNumber) {
                assertEquals(rowNumber + 1, ((Number) parameters.getParameter("i")).intValue());
                assertEquals(5, ((Number) parameters.getParameter("a")).intValue());
                assertEquals("test", parameters.getParameter("s"));
            }
        };
        newConnection().executeQuery(r, MockParametersCallbacks.fromMap(Collections.singletonMap("a0", 5)), callback);

    }

    /**
     * Tests if readers/writers are working
     */
    public void testStreamHandling() {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) throws IOException {
                return new ByteArrayInputStream("Hello".getBytes());
            }

            public OutputStream getOutputStream(final URL u) throws IOException {
                return os;
            }

            public int getContentLength(final URL u) {
                return 0;
            }
        };
        Connection c = new Driver().connect(
                new MockConnectionParameters(Collections.singletonMap(ScriptConnection.ENCODING, "UTF-8"), "tst:/file"));

        c.executeScript(new StringResource("print('Hello '+name)"),
                MockParametersCallbacks.fromMap(Collections.singletonMap("name", "world")));
        assertEquals("Hello world", os.toString());
    }

    public void testGetErrorStatement() {
        String st = ScriptConnection.getErrorStatement(
                new StringResource("a\nbb\ncc"), new ScriptException("test", "test", 2));
        assertEquals("bb", st);
    }

}
