/*
 * Copyright 2006-2007 The Scriptella Project Team.
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
import scriptella.configuration.MockConnectionEl;
import scriptella.configuration.StringResource;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.MockDriverContext;
import scriptella.spi.MockParametersCallbacks;
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
     * History:
     * 06.09.2006 - Duron 1.7Mhz - 656 ms
     */
    public void testScript() {
        JaninoConnection c = new JaninoConnection(new ConnectionParameters(new MockConnectionEl(), MockDriverContext.INSTANCE));
        ParametersCallback pc = MockParametersCallbacks.NAME;


        Resource scriptContent = new StringResource("int i=1;get(\"1\");");
        for (int i = 0; i < 1000000; i++) {
            c.executeScript(scriptContent, pc);
        }
        c.close();
    }

    /**
     * History:
     * 06.09.2006 - Duron 1.7Mhz - 844 ms
     */
    public void testQuery() {
        JaninoConnection c = new JaninoConnection(new ConnectionParameters(new MockConnectionEl(), MockDriverContext.INSTANCE));
        final int[] r = new int[]{0};

        Resource queryContent = new StringResource(
                "for (int i=0;i<1000000;i++) {" +
                        "set(\"p\", \"value\"+i );" +
                        "set(\"extra\", \"value\" );" +
                        "next();" +
                        "}");
        c.executeQuery(queryContent,
                MockParametersCallbacks.SIMPLE, new QueryCallback() {
            public final void processRow(final ParametersCallback parameters) {
                r[0]++;
                parameters.getParameter("p");
            }
        });
        c.close();
        assertEquals(1000000, r[0]);

    }


}
