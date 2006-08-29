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

import scriptella.AbstractTestCase;
import scriptella.spi.MockConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;

/**
 * Tests Janino connection class.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JaninoPerfTest extends AbstractTestCase {
    /**
     * This test creates a Janino connection that executes simple valid and invalid scripts.
     */
    public void testScript() {
        JaninoConnection c = new JaninoConnection(MockConnectionParameters.NULL);
        ParametersCallback pc = new ParametersCallback() {
            public final Object getParameter(final String name) {
                return name;
            }
        };


        Resource scriptContent = JaninoConnectionTest.asResource("int i=1;get(\"1\");");
        for (int i=0;i<1000000;i++) {
            c.executeScript(scriptContent, pc);
        }
        c.close();
    }

    public void testQuery() {
        JaninoConnection c = new JaninoConnection(MockConnectionParameters.NULL);
        final int[] r = new int[] {0};

        Resource queryContent = JaninoConnectionTest.asResource(
                "for (int i=0;i<1000000;i++) {" +
                    "set(\"p\", \"value\"+i );" +
                    "next();" +
                "}");
        c.executeQuery(queryContent,
                new ParametersCallback() {
            public final Object getParameter(final String name) {
                return "Param: "+name;
            }
        }, new QueryCallback() {
            public final void processRow(final ParametersCallback parameters) {
                r[0]++;
            }
        });
        c.close();
        assertEquals(1000000, r[0]);

    }


}
