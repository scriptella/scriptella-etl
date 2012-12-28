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
package scriptella.driver.jexl;

import scriptella.AbstractTestCase;
import scriptella.configuration.StringResource;
import scriptella.spi.IndexedQueryCallback;
import scriptella.spi.MockConnectionParameters;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;
import scriptella.spi.Resource;

import java.util.Collections;

/**
 * @author Fyodor Kupolov
 * @version 1.0
 * @since 12.01.2007
 */
public class JexlConnectionPerfTest extends AbstractTestCase {

    /**
     * History:
     * 12.01.2007 - Duron 1.7Mhz - 670 ms
     */
    public void testExecuteScript() {
        Resource r = new StringResource("x=0;while (x < 2000) {x=x+step;};");
        JexlConnection jc = new JexlConnection(new MockConnectionParameters());
        for (int i = 0; i < 200; i++) {
            jc.executeScript(r, MockParametersCallbacks.fromMap(Collections.singletonMap("step", 1)));
        }
    }

    /**
     * History:
     * 12.01.2007 - Duron 1.7Mhz - 720 ms
     */
    public void testExecuteQuery() {
        Resource r = new StringResource("i=0;while (i < maxI) {i=i+1;query.next();}");
        JexlConnection jc = new JexlConnection(new MockConnectionParameters());
        IndexedQueryCallback callback = new IndexedQueryCallback() {
            protected void processRow(final ParametersCallback parameters, final int rowNumber) {
                parameters.getParameter("i");
            }
        };
        for (int i=0;i<10;i++) {
            jc.executeQuery(r, MockParametersCallbacks.fromMap(Collections.singletonMap("maxI", 20000)), callback);
        }

    }

    /**
     * History:
     * 27.06.2010 - Core2 Duo 2.4Mhz - 730 ms
     */
    public void testNotExistingVariableInALoop() {
        Resource r = new StringResource("v=0;i=0 while(i<1000) { v=unknownVar+1;i=i+1};");
        JexlConnection jc = new JexlConnection(new MockConnectionParameters());
        for (int i = 0; i < 200; i++) {
            jc.executeScript(r, MockParametersCallbacks.NULL);
        }
    }


}
