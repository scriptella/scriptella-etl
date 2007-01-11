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
package scriptella.util;

import scriptella.AbstractTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Tests for {@link PropertiesMap}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class PropertiesMapTest extends AbstractTestCase {
    public void testLoad() throws IOException {
        PropertiesMap pm = new PropertiesMap();
        pm.load(new ByteArrayInputStream("p1=1\np1=11\np2=2".getBytes()));
        assertEquals(2, pm.size());
        assertEquals("1", pm.get("p1"));
        assertEquals("2", pm.get("p2"));
        assertEquals("p1", pm.keySet().iterator().next());
    }

    public void testPut() throws IOException {
        PropertiesMap pm = new PropertiesMap();
        pm.put("p2", "2");
        pm.put("p2", "");
        pm.put("p1", "1");
        assertEquals(2, pm.size());
        assertEquals("1", pm.get("p1"));
        assertEquals("2", pm.get("p2"));
        //Insertion order is preserved
        assertEquals("p2", pm.keySet().iterator().next());
        //Check if remove is working
        pm.remove("p1");
        pm.put("p1", "");
        assertEquals("", pm.get("p1"));

    }

}
