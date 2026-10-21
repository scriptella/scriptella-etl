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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import scriptella.core.EtlCancelledException;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Token traversal retains only path state and one selected record. */
final class JsonQuery {
    private final JsonParser parser;
    private final JsonPath path;
    private final ParametersCallback parent;
    private final QueryCallback callback;
    private long rows;

    JsonQuery(JsonParser parser, JsonPath path, ParametersCallback parent, QueryCallback callback) {
        this.parser = parser;
        this.path = path;
        this.parent = parent;
        this.callback = callback;
    }

    void execute() throws IOException {
        JsonToken root = next();
        if (root == null) {
            throw error("Empty JSON document");
        }
        if (path.segments.isEmpty() && root == JsonToken.START_OBJECT) {
            emit(value());
        } else {
            select(0);
        }
        if (next() != null) {
            throw error("Additional content after JSON document");
        }
    }

    Object readRecord() throws IOException {
        if (next() == null) {
            throw error("Empty JSON Lines record");
        }
        Object record = value();
        if (next() != null) {
            throw error("Additional content after JSON Lines record");
        }
        return record;
    }

    private JsonToken next() throws IOException {
        EtlCancelledException.checkEtlCancelled();
        return parser.nextToken();
    }

    private void select(int depth) throws IOException {
        if (depth == path.segments.size()) {
            if (parser.currentToken() != JsonToken.START_ARRAY) {
                throw error("Selected value must be an array");
            }
            while (next() != JsonToken.END_ARRAY) {
                emit(value());
            }
            return;
        }
        Object segment = path.segments.get(depth);
        boolean found = false;
        if (segment instanceof String && parser.currentToken() == JsonToken.START_OBJECT) {
            while (next() != JsonToken.END_OBJECT) {
                String key = parser.currentName();
                next();
                if (segment.equals(key)) {
                    if (found) {
                        throw error("Repeated selected ancestor key");
                    }
                    found = true;
                    select(depth + 1);
                } else {
                    skip();
                }
            }
        } else if (segment instanceof Integer && parser.currentToken() == JsonToken.START_ARRAY) {
            long index = 0;
            while (next() != JsonToken.END_ARRAY) {
                if (index++ == ((Integer) segment).intValue()) {
                    found = true;
                    select(depth + 1);
                } else {
                    skip();
                }
            }
        } else {
            throw error("Wrong container type at selection segment " + (depth + 1));
        }
        if (!found) {
            throw error("Missing selection segment " + (depth + 1));
        }
    }

    private void skip() throws IOException {
        int depth = parser.currentToken().isStructStart() ? 1 : 0;
        // Finish lazy string tokens too, validating escapes and string constraints.
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            parser.finishToken();
        }
        while (depth > 0) {
            JsonToken token = next();
            if (token == null) {
                throw error("Unexpected end of JSON document");
            }
            if (token.isStructStart()) {
                depth++;
            } else if (token.isStructEnd()) {
                depth--;
            } else if (token == JsonToken.VALUE_STRING) {
                parser.finishToken();
            }
        }
    }

    private Object value() throws IOException {
        EtlCancelledException.checkEtlCancelled();
        JsonToken token = parser.currentToken();
        if (token == null) {
            throw error("Unexpected end of JSON record");
        }
        switch (token) {
            case START_OBJECT:
                Map<String, Object> map = new LinkedHashMap<String, Object>();
                while (next() != JsonToken.END_OBJECT) {
                    String key = parser.currentName();
                    if (map.containsKey(key)) {
                        throw error("Duplicate object key in selected record");
                    }
                    next();
                    map.put(key, value());
                }
                return map;
            case START_ARRAY:
                List<Object> list = new ArrayList<Object>();
                while (next() != JsonToken.END_ARRAY) {
                    list.add(value());
                }
                return list;
            case VALUE_STRING:
                return parser.getText();
            case VALUE_NUMBER_INT:
                BigInteger integer = parser.getBigIntegerValue();
                if (integer.bitLength() < 64) {
                    return Long.valueOf(integer.longValue());
                }
                return integer;
            case VALUE_NUMBER_FLOAT:
                return parser.getDecimalValue();
            case VALUE_TRUE:
                return Boolean.TRUE;
            case VALUE_FALSE:
                return Boolean.FALSE;
            case VALUE_NULL:
                return null;
            default:
                throw error("Expected a JSON value");
        }
    }

    private void emit(final Object row) {
        EtlCancelledException.checkEtlCancelled();
        rows++;
        callback.processRow(new ParametersCallback() {
            public Object getParameter(String name) {
                return "row".equals(name) ? row : parent.getParameter(name);
            }
        });
    }

    private JsonProviderException error(String message) {
        return new JsonProviderException(message + " at line " + parser.currentLocation().getLineNr()
                + ", column " + parser.currentLocation().getColumnNr() + ", after " + rows + " rows");
    }
}
