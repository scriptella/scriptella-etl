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
package scriptella.driver.janino;

import org.codehaus.commons.compiler.CompileException;
import scriptella.AbstractTestCase;
import scriptella.configuration.MockConnectionEl;
import scriptella.configuration.StringResource;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.MockDriverContext;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Characterization of the Janino ScriptEvaluator integration used by Scriptella.
 * <p>Janino 3.1.x patch upgrades must keep compile inheritance and query APIs stable.
 *
 * @author Scriptella Project Team
 */
public class JaninoCompilerContractTest extends AbstractTestCase {
    public static int marker;

    public void testScriptExtendsJaninoScript() {
        JaninoConnection c = new JaninoConnection(
                new ConnectionParameters(new MockConnectionEl(), MockDriverContext.INSTANCE));
        marker = 0;
        c.executeScript(new StringResource(
                JaninoCompilerContractTest.class.getName() + ".marker = 7;"), null);
        c.close();
        assertEquals(7, marker);
    }

    public void testQueryNextAndParameterAccess() {
        JaninoConnection c = new JaninoConnection(
                new ConnectionParameters(new MockConnectionEl(), MockDriverContext.INSTANCE));
        final List<String> rows = new ArrayList<String>();
        c.executeQuery(new StringResource(
                        "set(\"p\", \"x\"+get(\"p\")); next();"),
                MockParametersCallbacks.SIMPLE, new QueryCallback() {
                    public void processRow(final ParametersCallback parameters) {
                        rows.add(String.valueOf(parameters.getParameter("p")));
                    }
                });
        c.close();
        assertEquals(1, rows.size());
        assertEquals("x*p*", rows.get(0));
    }

    public void testCompileFailureExposesNativeCompileException() {
        JaninoConnection c = new JaninoConnection(
                new ConnectionParameters(new MockConnectionEl(), MockDriverContext.INSTANCE));
        try {
            c.executeScript(new StringResource("this is not java ;;;"), null);
            fail("Invalid Janino script must fail");
        } catch (ProviderException e) {
            assertTrue(e.getNativeException() instanceof CompileException
                    || e.getCause() instanceof CompileException
                    || String.valueOf(e.getNativeException()).contains("Compile")
                    || e.getMessage().toLowerCase().contains("compilation"));
        } finally {
            c.close();
        }
    }
}
