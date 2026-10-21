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
import scriptella.expression.Expression;
import scriptella.spi.ParametersCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compiled structural template. Only placeholder syntax is scanned here; Jackson validates JSON. */
final class JsonTemplate {
    private final String source;
    private final Map<Long, Expression> expressions = new HashMap<Long, Expression>();
    private final Node root;
    private int usedExpressions;

    JsonTemplate(String source) {
        this.source = source;
        String masked = mask();
        try (JsonParser parser = JsonConnection.FACTORY.createParser(masked)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw error("Template must contain one root object", 0, "$", null);
            }
            root = parse(parser, "$");
            if (parser.nextToken() != null || usedExpressions != expressions.size()) {
                throw error("Extra root content", parser.currentTokenLocation().getCharOffset(), "$", null);
            }
        } catch (IOException e) {
            long offset = e instanceof com.fasterxml.jackson.core.JsonProcessingException
                    ? ((com.fasterxml.jackson.core.JsonProcessingException) e).getLocation().getCharOffset() : 0;
            throw error("Invalid JSON template", offset, "$", e);
        }
    }

    boolean matches(String text) {
        return source.equals(text);
    }

    Object evaluate(ParametersCallback parameters) {
        return root.evaluate(parameters);
    }

    private String mask() {
        char[] chars = source.toCharArray();
        boolean string = false;
        for (int i = 0; i < chars.length; i++) {
            char c = source.charAt(i);
            if (string) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    string = false;
                }
            } else if (c == '"') {
                string = true;
            } else if (c == '?' && i + 1 < chars.length && chars[i + 1] == '{') {
                int end = expressionEnd(i);
                String expression = source.substring(i + 2, end).trim();
                if (expression.isEmpty()) {
                    throw error("Empty placeholder", i, "$", null);
                }
                try {
                    expressions.put((long) i, Expression.compile(expression));
                } catch (Expression.ParseException e) {
                    throw error("Invalid expression", i, "$", e);
                }
                // A single numeric token preserves every source offset, including newlines.
                chars[i] = '0';
                for (int j = i + 1; j <= end; j++) {
                    if (chars[j] != '\n' && chars[j] != '\r') {
                        chars[j] = ' ';
                    }
                }
                i = end;
            }
        }
        return new String(chars);
    }

    private int expressionEnd(int start) {
        StringBuilder stack = new StringBuilder("}");
        char quote = 0;
        boolean lineComment = false;
        boolean blockComment = false;
        for (int i = start + 2; i < source.length(); i++) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : 0;
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (lineComment) {
                if (c == '\n' || c == '\r') {
                    lineComment = false;
                }
            } else if (blockComment) {
                if (c == '*' && next == '/') {
                    blockComment = false;
                    i++;
                }
            } else if (c == '/' && (next == '/' || next == '*')) {
                lineComment = next == '/';
                blockComment = next == '*';
                i++;
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '{' || c == '[' || c == '(') {
                stack.append(c == '{' ? '}' : c == '[' ? ']' : ')');
            } else if (c == '}' || c == ']' || c == ')') {
                if (stack.charAt(stack.length() - 1) != c) {
                    throw error("Unbalanced placeholder", i, "$", null);
                }
                stack.setLength(stack.length() - 1);
                if (stack.length() == 0) {
                    return i;
                }
            }
        }
        throw error("Unterminated placeholder", start, "$", null);
    }

    private Node parse(JsonParser parser, String path) throws IOException {
        long offset = parser.currentTokenLocation().getCharOffset();
        Expression expression = expressions.get(offset);
        if (expression != null) {
            usedExpressions++;
            return parameters -> {
                try {
                    Object value = expression.evaluate(parameters);
                    // Validate here to retain the expression's source location and nested output path.
                    JsonValues.validate(value, path);
                    return value;
                } catch (scriptella.core.EtlCancelledException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw error("Invalid placeholder value", offset, path, e);
                }
            };
        }
        switch (parser.currentToken()) {
            case START_OBJECT:
                Map<String, Node> fields = new LinkedHashMap<String, Node>();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String key = parser.currentName();
                    if (fields.containsKey(key)) {
                        throw error("Duplicate object key", parser.currentTokenLocation().getCharOffset(), path, null);
                    }
                    parser.nextToken();
                    fields.put(key, parse(parser, path + "[" + key + "]"));
                }
                return parameters -> {
                    Map<String, Object> result = new LinkedHashMap<String, Object>();
                    for (Map.Entry<String, Node> field : fields.entrySet()) {
                        result.put(field.getKey(), field.getValue().evaluate(parameters));
                    }
                    return result;
                };
            case START_ARRAY:
                List<Node> items = new ArrayList<Node>();
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    items.add(parse(parser, path + "[" + items.size() + "]"));
                }
                return parameters -> {
                    List<Object> result = new ArrayList<Object>();
                    for (Node item : items) {
                        result.add(item.evaluate(parameters));
                    }
                    return result;
                };
            case VALUE_STRING:
                String text = parser.getText();
                return parameters -> text;
            case VALUE_NUMBER_INT:
                java.math.BigInteger integer = parser.getBigIntegerValue();
                return parameters -> integer;
            case VALUE_NUMBER_FLOAT:
                java.math.BigDecimal decimal = parser.getDecimalValue();
                return parameters -> decimal;
            case VALUE_TRUE:
                return parameters -> Boolean.TRUE;
            case VALUE_FALSE:
                return parameters -> Boolean.FALSE;
            case VALUE_NULL:
                return parameters -> null;
            default:
                throw error("Expected JSON value", offset, path, null);
        }
    }

    private JsonProviderException error(String message, long offset, String path, Throwable cause) {
        int line = 1;
        int column = 1;
        for (int i = 0; i < offset && i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new JsonProviderException(message + " at template line " + line + ", column " + column
                + ", output path " + path, cause);
    }

    private interface Node {
        Object evaluate(ParametersCallback parameters);
    }
}
