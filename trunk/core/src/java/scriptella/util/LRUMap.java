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
package scriptella.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents LRU Map implementation based on {@link java.util.LinkedHashMap}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class LRUMap<K, V> extends LinkedHashMap<K, V> {
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final long serialVersionUID = 1;
    private int size;

    public LRUMap(int size) {
        super(size, DEFAULT_LOAD_FACTOR, true);
        this.size = size;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        boolean remove = size() > size;
        if (remove) {
            onEldestEntryRemove(eldest);
        }
        return remove;
    }

    /**
     * Invoked when eldest entry is about to be removed.
     *
     * @param eldest eldest entry.
     */
    protected void onEldestEntryRemove(Map.Entry<K, V> eldest) {
    }

}
