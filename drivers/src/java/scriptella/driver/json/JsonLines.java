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
import scriptella.core.EtlCancelledException;
import scriptella.jdbc.JdbcUtils;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

/** Physical-line framing; values and callbacks use the standard JSON reader. */
final class JsonLines {
    private JsonLines() {
    }

    static void execute(Reader source, ParametersCallback parameters, QueryCallback callback, URL url) throws IOException {
        BufferedReader input = new BufferedReader(source);
        long line = 1;
        while (true) {
            EtlCancelledException.checkEtlCancelled();
            StringBuilder text = new StringBuilder();
            boolean eof = false;
            final Object record;
            try {
                while (true) {
                    EtlCancelledException.checkEtlCancelled();
                    int ch = input.read();
                    if (ch == -1) {
                        eof = true;
                        break;
                    }
                    if (ch == '\n') {
                        break;
                    }
                    if (ch == '\r') {
                        if (input.read() != '\n') {
                            throw new JsonProviderException("Expected LF after CR");
                        }
                        break;
                    }
                    text.append((char) ch);
                }
                if (eof && text.length() == 0) {
                    return;
                }
                try (JsonParser parser = JsonConnection.FACTORY.createParser(text.toString())) {
                    record = new JsonQuery(parser, JsonPath.parse(""), parameters, callback).readRecord();
                }
            } catch (IOException e) {
                throw new JsonProviderException("Cannot read JSON Lines at line " + line + ", record " + line, JdbcUtils.sanitize(e, url));
            } catch (JsonProviderException e) {
                throw new JsonProviderException("Invalid JSON Lines at line " + line + ", record " + line, JdbcUtils.sanitize(e, url));
            }
            EtlCancelledException.checkEtlCancelled();
            callback.processRow(name -> "row".equals(name) ? record : parameters.getParameter(name));
            line++;
            if (eof) {
                return;
            }
        }
    }
}
