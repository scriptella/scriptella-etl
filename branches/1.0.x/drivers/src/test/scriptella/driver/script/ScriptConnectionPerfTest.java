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
package scriptella.driver.script;

import scriptella.AbstractTestCase;
import scriptella.configuration.StringResource;
import scriptella.spi.IndexedQueryCallback;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;
import scriptella.spi.Resource;

import java.util.Collections;

/**
 * Performance tests for {@link scriptella.driver.script.ScriptConnection}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 * @since 12.01.2007
 */
public class ScriptConnectionPerfTest extends AbstractTestCase {

    /**
     * History:
     * 13.05.2007 - Duron 1.7Mhz - 1125 ms
     */
    public void testExecuteScript() {
        Resource r = new StringResource("x=0;while (x < 500) {x+=step;};");
        ScriptConnection c = ScriptConnectionTest.newConnection();
        for (int i = 0; i < 100; i++) {
            c.executeScript(r, MockParametersCallbacks.fromMap(Collections.singletonMap("step", 1)));
        }
    }

    /**
     * History:
     * 13.05.2007 - Duron 1.7Mhz - 735 ms
     */
    public void testExecuteQuery() {
        Resource r = new StringResource("for (var i=0;i<maxI;i++) {query.next();}");
        ScriptConnection c = ScriptConnectionTest.newConnection();
        IndexedQueryCallback callback = new IndexedQueryCallback() {
            protected void processRow(final ParametersCallback parameters, final int rowNumber) {
                parameters.getParameter("i");
            }
        };
        for (int i = 0; i < 10; i++) {
            c.executeQuery(r, MockParametersCallbacks.fromMap(Collections.singletonMap("maxI", 10000)), callback);
        }

    }

}
