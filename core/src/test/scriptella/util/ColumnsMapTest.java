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

/**
 * Tests for {@link ColumnsMap}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ColumnsMapTest extends AbstractTestCase {
    public void test() {
        ColumnsMap map = new ColumnsMap();
        map.registerColumn("col1", 1);
        try {
            map.registerColumn("cOL1", 0);
            fail("Index 0 should cause IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            //OK
        }
        map.registerColumn("col2", 2);
        map.registerColumn("COl4", 4);
        assertNull(map.find("no"));
        assertEquals(2, map.find("col2").intValue());
        assertEquals(1, map.find("col1").intValue());
        assertEquals(4, map.find("4").intValue());
        assertEquals(4, map.find("CoL4").intValue());
        assertEquals(3, map.find("3").intValue());
    }
}
