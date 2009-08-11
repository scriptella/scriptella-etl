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
package scriptella.driver.jexl;

import scriptella.AbstractTestCase;
import scriptella.configuration.MockConnectionEl;
import scriptella.configuration.StringResource;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.IndexedQueryCallback;
import scriptella.spi.MockDriverContext;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;
import scriptella.spi.Resource;

import java.util.Collections;

/**
 * Tests for {@link JexlConnection}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 * @since 11.01.2007
 */
public class JexlConnectionTest extends AbstractTestCase {
    private Object v;
    public void setValue(Object v) {
        this.v = v;
    }

    public void testExecuteScript() {
        v=null;
        Resource r = new StringResource("x=0;while (x < 10) {x=x+2;};obj.setValue(x)");
        JexlConnection jc = new JexlConnection(new ConnectionParameters(new MockConnectionEl(), new MockDriverContext()));
        jc.executeScript(r, MockParametersCallbacks.fromMap(Collections.singletonMap("obj", this)));
        assertEquals(10, ((Number)v).intValue());
    }

    public void testExecuteQuery() {
        Resource r = new StringResource("i=0;a=a0;s='test';while (i < 10) {i=i+1;query.next();}");
        JexlConnection jc = new JexlConnection(new ConnectionParameters(new MockConnectionEl(), new MockDriverContext()));
        IndexedQueryCallback callback = new IndexedQueryCallback() {
            protected void processRow(final ParametersCallback parameters, final int rowNumber) {
                assertEquals(rowNumber+1, ((Number)parameters.getParameter("i")).intValue());
                assertEquals(5, ((Number)parameters.getParameter("a")).intValue());
                assertEquals("test", parameters.getParameter("s"));
            }
        };
        jc.executeQuery(r, MockParametersCallbacks.fromMap(Collections.singletonMap("a0", 5)), callback);

    }

}
