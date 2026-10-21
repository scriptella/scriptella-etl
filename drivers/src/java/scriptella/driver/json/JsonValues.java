/*
 * Copyright 2026 The Scriptella Project Team.
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
package scriptella.driver.json;

import com.fasterxml.jackson.core.JsonGenerator;
import scriptella.core.EtlCancelledException;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Explicit JSON value policy; no bean conversion or implicit stringification. */
final class JsonValues {
    static void validate(Object value, String path) {
        try {
            write(value, null, path, new IdentityHashMap<Object, Boolean>(), 0);
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    static String serialize(Object value) throws IOException {
        StringWriter buffer = new StringWriter();
        try (JsonGenerator generator = JsonConnection.FACTORY.createGenerator(buffer)) {
            write(value, generator, "$", new IdentityHashMap<Object, Boolean>(), 0);
        }
        return buffer.toString();
    }

    private static void write(Object value, JsonGenerator out, String path,
                              IdentityHashMap<Object, Boolean> ancestors, int depth) throws IOException {
        EtlCancelledException.checkEtlCancelled();
        if (depth > 500) {
            throw failure("Value nesting exceeds 500", path);
        }
        if (value == null) {
            if (out != null) {
                out.writeNull();
            }
        } else if (value instanceof String) {
            if (out != null) {
                out.writeString((String) value);
            }
        } else if (value instanceof Boolean) {
            if (out != null) {
                out.writeBoolean((Boolean) value);
            }
        } else if (value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof BigInteger || value instanceof BigDecimal
                || value instanceof Float || value instanceof Double) {
            if ((value instanceof Float && !Float.isFinite((Float) value))
                    || (value instanceof Double && !Double.isFinite((Double) value))) {
                throw failure("Non-finite number", path);
            }
            if (out != null) {
                out.writeNumber(value.toString());
            }
        } else if (value instanceof Map || value instanceof List) {
            if (ancestors.put(value, Boolean.TRUE) != null) {
                throw failure("Cyclic value", path);
            }
            if (value instanceof Map) {
                if (out != null) {
                    out.writeStartObject();
                }
                Set<String> keys = new HashSet<String>();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    if (!(entry.getKey() instanceof String) || !keys.add((String) entry.getKey())) {
                        throw failure("Map keys must be unique non-null strings", path);
                    }
                    String key = (String) entry.getKey();
                    if (out != null) {
                        out.writeFieldName(key);
                    }
                    write(entry.getValue(), out, path + "[" + key + "]", ancestors, depth + 1);
                }
                if (out != null) {
                    out.writeEndObject();
                }
            } else {
                if (out != null) {
                    out.writeStartArray();
                }
                int index = 0;
                for (Object item : (List<?>) value) {
                    write(item, out, path + "[" + index++ + "]", ancestors, depth + 1);
                }
                if (out != null) {
                    out.writeEndArray();
                }
            }
            ancestors.remove(value);
        } else {
            throw failure("Unsupported value type " + value.getClass().getName(), path);
        }
    }

    private static JsonProviderException failure(String message, String path) {
        return new JsonProviderException(message + " at output path " + path);
    }
}
