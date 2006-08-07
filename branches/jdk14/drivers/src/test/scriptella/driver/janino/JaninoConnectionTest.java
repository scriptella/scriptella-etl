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
package scriptella.driver.janino;

import org.codehaus.janino.CompileException;
import scriptella.AbstractTestCase;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests Janino connection class.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JaninoConnectionTest extends AbstractTestCase {
    public static int field; //Field to be filled by a script

    /**
     * This test creates a Janino connection that executes simple valid and invalid scripts.
     */
    public void testScript() {
        JaninoConnection c = new JaninoConnection();
        field = 0;
        c.executeScript(asResource(JaninoConnectionTest.class.getName() + ".field=1;"), null);
        try {
            c.executeScript(asResource(JaninoConnectionTest.class.getName() + ".nosuchfield=1;"), null);
            fail("This script should fail");
        } catch (ProviderException e) {
            Throwable ne = e.getNativeException();
            assertTrue(ne !=null && ne instanceof CompileException);
            //OK
        }
        c.close();
        assertEquals(1, field);
    }

    public void testQuery() {
        JaninoConnection c = new JaninoConnection();
        field = 0;
        final List<String> rows = new ArrayList<String>();

        c.executeQuery(asResource(
                "set(\"p\", \"//\"+get(\"p\") );" +
                "next();" +
                "next(new String[] {\"p\"}, new Object[] {\"v2\"});"),
                new ParametersCallback() {
            public Object getParameter(final String name) {
                return "Param: "+name;
            }
        }, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                rows.add(parameters.getParameter("p").toString());
            }
        });
        c.close();
        List<String> expected = Arrays.asList("//Param: p", "v2");
        assertEquals(expected, rows);

    }


    /**
     * @return text as resource
     */
    static Resource asResource(final String text) {
        return new Resource() {
            public Reader open() {
                return new StringReader(text);
            }
        };
    }
}