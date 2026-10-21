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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Small collection path grammar, independent of expression evaluation. */
final class JsonPath {
    final List<Object> segments = new ArrayList<Object>();

    static JsonPath parse(String text) {
        JsonPath result = new JsonPath();
        int i = 0;
        while (i < text.length()) {
            if (i > 0 && text.charAt(i) == '.') {
                i++;
                if (i == text.length() || !start(text.charAt(i))) {
                    throw invalid(i);
                }
            } else if (i > 0 && text.charAt(i) != '[') {
                throw invalid(i);
            }
            if (start(text.charAt(i))) {
                int begin = i++;
                while (i < text.length() && (start(text.charAt(i)) || digit(text.charAt(i)))) {
                    i++;
                }
                result.segments.add(text.substring(begin, i));
            } else if (text.charAt(i) == '[') {
                i++;
                if (i >= text.length()) {
                    throw invalid(i);
                }
                char quote = text.charAt(i);
                if (quote == 39 || quote == 34) {
                    int begin = i++;
                    boolean ended = false;
                    while (i < text.length()) {
                        char c = text.charAt(i++);
                        if (c == 92) {
                            if (i == text.length()) {
                                throw invalid(i);
                            }
                            i++;
                        } else if (c == quote) {
                            ended = true;
                            break;
                        }
                    }
                    if (!ended) {
                        throw invalid(i);
                    }
                    try (JsonParser parser = JsonConnection.FACTORY.createParser(text.substring(begin, i))) {
                        if (quote == 39) {
                            parser.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
                        }
                        if (parser.nextToken() != JsonToken.VALUE_STRING) {
                            throw invalid(i);
                        }
                        result.segments.add(parser.getText());
                        if (parser.nextToken() != null) {
                            throw invalid(i);
                        }
                    } catch (IOException e) {
                        throw new JsonProviderException("Invalid quoted JSON selection key at column " + i, e);
                    }
                } else {
                    int begin = i;
                    while (i < text.length() && digit(text.charAt(i))) {
                        i++;
                    }
                    String index = text.substring(begin, i);
                    if (index.isEmpty() || (index.length() > 1 && index.charAt(0) == '0')) {
                        throw invalid(i);
                    }
                    try {
                        result.segments.add(Integer.valueOf(index));
                    } catch (NumberFormatException e) {
                        throw invalid(i);
                    }
                }
                if (i >= text.length() || text.charAt(i++) != ']') {
                    throw invalid(i);
                }
            } else {
                throw invalid(i);
            }
        }
        return result;
    }

    private static boolean start(char c) {
        return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c == '_';
    }

    private static boolean digit(char c) {
        return c >= '0' && c <= '9';
    }

    private static JsonProviderException invalid(int offset) {
        return new JsonProviderException("Invalid JSON selection path at column " + (offset + 1));
    }
}
