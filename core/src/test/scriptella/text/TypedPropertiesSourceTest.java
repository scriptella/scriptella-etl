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

package scriptella.text;

import junit.framework.TestCase;
import scriptella.configuration.ConfigurationException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class TypedPropertiesSourceTest extends TestCase {
    public void testParseBooleanProperty() {
        Map<String, Object> p = new HashMap<String, Object>();
        TypedPropertiesSource ps = new TypedPropertiesSource(p);

        p.put("v", " true");
        assertTrue(ps.getBooleanProperty("v", false));

        p.put("v", " 1");
        assertTrue(ps.getBooleanProperty("v", false));

        p.put("v", " on");
        assertTrue(ps.getBooleanProperty("v", false));

        p.put("v", " yes");
        assertTrue(ps.getBooleanProperty("v", false));

        p.put("v", " false");
        assertFalse(ps.getBooleanProperty("v", false));

        p.put("v", " 0");
        assertFalse(ps.getBooleanProperty("v", false));

        p.put("v", " off");
        assertFalse(ps.getBooleanProperty("v", false));

        p.put("v", " no");
        assertFalse(ps.getBooleanProperty("v", false));

        p.put("v", " undefined");
        try {
            ps.getBooleanProperty("v", false);
            fail("Exception is expected for unrecognized value");
        } catch (ConfigurationException e) {
            //OK
        }

    }
}
