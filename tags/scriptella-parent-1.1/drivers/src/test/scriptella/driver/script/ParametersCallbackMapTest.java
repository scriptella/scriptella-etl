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

import junit.framework.TestCase;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;

import java.util.Collections;

/**
 * @author Fyodor Kupolov
 */
public class ParametersCallbackMapTest extends TestCase {
    public void testGet() {
        ParametersCallback pc = MockParametersCallbacks.fromMap(Collections.singletonMap("var", "value"));
        ParametersCallbackMap map = new ParametersCallbackMap(pc);
        assertEquals("value", map.get("var"));

        //Test override
        map.put("var", "value2");
        assertEquals("value2", map.get("var"));

        map.put("var", null);
        assertNull(map.get("var"));
    }
}
