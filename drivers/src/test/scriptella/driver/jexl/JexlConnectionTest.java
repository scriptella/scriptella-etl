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
package scriptella.driver.jexl;

import scriptella.AbstractTestCase;
import scriptella.configuration.MockConnectionEl;
import scriptella.configuration.StringResource;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.MockDriverContext;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.Resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
        Map<String, Object> m = new HashMap<String, Object>();
        jc.executeScript(r, MockParametersCallbacks.fromMap(Collections.singletonMap("obj", this)));
        assertEquals(10, ((Number)v).intValue());

    }
}
