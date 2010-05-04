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
package scriptella.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a map of columns accessible by name and index.
 * <p>This class is useful for queries producing results similar to ResultSet.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ColumnsMap {
    private Map<String, Integer> map;

    /**
     * Registers column to for later lookup.
     *
     * @param name  column name.
     * @param index positive column index.
     * @throws IllegalArgumentException if index has illegal value
     */
    public void registerColumn(String name, int index) throws IllegalArgumentException {
        if (index <= 0) {
            throw new IllegalArgumentException("Index must be positive integer");
        }
        if (map == null) {
            map = CollectionUtils.newCaseInsensitiveAsciiMap();
        }
        map.put(name, index);
    }

    /**
     * Finds column index by name.
     *
     * @param name column name.
     * @return column index, or null if column not found.
     */
    public Integer find(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter name cannot be null");
        }
        Integer index = map == null ? null : map.get(name);
        //If name is not a column name and is integer
        if (index == null && StringUtils.isDecimalInt(name)) {
            index = Integer.valueOf(name); //Try to parse name as index
        }
        return index;
    }

    /**
     * Converts this mapping to index->name column map.
     * @return index->name column map.
     */
    public Map<Integer, String> asIndexNameMap() {
        if (map==null) {
            return Collections.emptyMap();
        }
        Map<Integer, String> m = new HashMap<Integer, String>();

        for (Map.Entry<String, Integer> e : map.entrySet()) {
            m.put(e.getValue(), e.getKey());
        }
        return m;
    }
}
