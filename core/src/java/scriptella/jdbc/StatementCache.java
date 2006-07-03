/*
 * Copyright 2006 The Scriptella Project Team.
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
package scriptella.jdbc;

import java.io.Closeable;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class StatementCache implements Closeable {
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private Map<String, PreparedStatement> map;
    private int maxSize;
    private List<PreparedStatement> releasedStatements = new ArrayList<PreparedStatement>();

    public StatementCache(final int size) {
        this.maxSize = size;
        this.map = new LinkedHashMap<String, PreparedStatement>(size, DEFAULT_LOAD_FACTOR, true) {
            protected boolean removeEldestEntry(Map.Entry<String, PreparedStatement> eldest) {
                boolean remove = size() > size;
                if (remove) {
                    releasedStatements.add(eldest.getValue());
                }

                return remove;
            }
        };
    }

    public int getMaxSize() {
        return maxSize;
    }

    public PreparedStatement get(String key) {
        return map.get(key);
    }

    public void put(String key, PreparedStatement entry) {
        map.put(key, entry);
    }

    /**
     * Invoke close method on statements pending release after removing from cache.
     */
    public void closeRemovedStatements() {
        if (!releasedStatements.isEmpty()) {
            close(releasedStatements);
            releasedStatements.clear();
        }
    }

    private static void close(Collection<PreparedStatement> list) {
        for (PreparedStatement ps: list) {
            JDBCUtils.closeSilent(ps);
        }
    }

    public void close() {
        if (map!=null) {
            closeRemovedStatements();
            close(map.values());
            map=null;
        }
    }
}
