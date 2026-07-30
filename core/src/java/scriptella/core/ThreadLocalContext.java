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
package scriptella.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Shares execution-scoped values with drivers loaded by a child-first driver
 * classloader.
 *
 * @author Scriptella Project Team
 */
public final class ThreadLocalContext {
    private static final ThreadLocal<Map<String, Object>> VALUES =
            new ThreadLocal<Map<String, Object>>();

    private ThreadLocalContext() {
    }

    public static void set(String key, Object value) {
        Map<String, Object> values = VALUES.get();
        if (value == null) {
            if (values != null) {
                values.remove(key);
                if (values.isEmpty()) {
                    VALUES.remove();
                }
            }
        } else {
            if (values == null) {
                values = new HashMap<String, Object>();
                VALUES.set(values);
            }
            values.put(key, value);
        }
    }

    public static Object get(String key) {
        Map<String, Object> values = VALUES.get();
        return values == null ? null : values.get(key);
    }
}
