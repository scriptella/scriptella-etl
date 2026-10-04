/*
 * Copyright 2006-2026 The Scriptella Project Team.
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
package scriptella.expression;

import scriptella.configuration.MissingEnvironmentVariableException;
import scriptella.spi.ParametersCallback;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Set;

/** Reserved, read-only process environment namespace shared by expressions and substitution. */
final class EnvironmentParameters implements ParametersCallback {
    private final ParametersCallback callback;
    private MissingEnvironmentVariableException failure;
    private boolean environmentUsed;
    private final java.util.Map<String, String> env = Collections.unmodifiableMap(new AbstractMap<String, String>() {
        @Override
        public String get(Object key) {
            String name = String.valueOf(key);
            String value = System.getenv(name);
            if (value == null) {
                failure = new MissingEnvironmentVariableException(name);
                throw failure;
            }
            return value;
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            // Do not enumerate or render environment values in diagnostics.
            return Collections.emptySet();
        }

        @Override
        public String toString() {
            return "[environment]";
        }
    });

    EnvironmentParameters(ParametersCallback callback) {
        this.callback = callback;
    }

    boolean isEnvironmentUsed() {
        return environmentUsed;
    }

    void check() {
        if (failure != null) {
            throw failure;
        }
    }

    public Object getParameter(String name) {
        if ("env".equals(name)) {
            environmentUsed = true;
            return env;
        }
        if (name.startsWith("env.")) {
            environmentUsed = true;
            return env.get(name.substring(4));
        }
        return callback.getParameter(name);
    }

    static Object get(ParametersCallback callback, String name) {
        if ("env".equals(name) || name.startsWith("env.")) {
            return new EnvironmentParameters(callback).getParameter(name);
        }
        return callback.getParameter(name);
    }
}
