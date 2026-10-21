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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import scriptella.core.EtlCancelledException;
import scriptella.jdbc.JdbcUtils;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.IOUtils;

import java.io.FilterInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.FilterOutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Streaming input or lazily opened output; construction performs no I/O. */
public class JsonConnection extends AbstractConnection {

    @Override
    public StatementCounter getProgressCounter() {
        return counter;
    }
    // Disable symbol tables: memory must not grow with distinct keys across records.
    static final JsonFactory FACTORY = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(1000).maxNumberLength(1000)
                    .maxStringLength(20000000).maxNameLength(50000).build())
            .disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES)
            .disable(JsonFactory.Feature.INTERN_FIELD_NAMES).build();
    private final URL url;
    private final Charset encoding;
    private final boolean jsonLines;
    private boolean reading;
    private boolean writing;
    private boolean finished;
    private boolean outputFailed;
    private Writer output;
    private JsonTemplate template;
    private boolean closed;

    public JsonConnection(ConnectionParameters parameters) {
        super(Driver.DIALECT, parameters);
        String format = parameters.getStringProperty("format");
        if (format != null && !"json".equals(format) && !"jsonl".equals(format)) {
            throw new JsonProviderException("Unsupported JSON format: " + format);
        }
        url = parameters.getUrl() == null ? null : parameters.getResolvedUrl();
        String filename = url == null ? "" : url.getPath().toLowerCase(Locale.ROOT);
        jsonLines = format == null ? filename.endsWith(".jsonl") || filename.endsWith(".ndjson")
                : "jsonl".equals(format);
        String charset = parameters.getCharsetProperty("encoding");
        encoding = charset == null ? StandardCharsets.UTF_8 : Charset.forName(charset);
    }

    protected Reader newInputReader() throws IOException {
        InputStream stream = url == null ? new FilterInputStream(System.in) {
            @Override
            public void close() {
                // Closing a query must not close process-wide standard input.
            }
        } : url.openStream();
        return new InputStreamReader(stream, encoding.newDecoder());
    }

    public void executeQuery(Resource query, ParametersCallback parameters, QueryCallback callback) {
        checkOpen();
        if (writing) {
            throw new JsonProviderException("Cannot read and write through the same JSON connection");
        }
        reading = true;
        EtlCancelledException.checkEtlCancelled();
        final JsonPath path;
        try (Reader content = query.open()) {
            path = JsonPath.parse(IOUtils.toString(content).trim());
        } catch (IOException e) {
            throw new JsonProviderException("Cannot read JSON selection path", e);
        }
        if (jsonLines && !path.segments.isEmpty()) {
            throw new JsonProviderException("JSON Lines requires an empty selection query");
        }
        if (jsonLines) {
            try (Reader input = new FilterReader(newInputReader()) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } catch (IOException e) {
                        // Sanitize before cleanup is suppressed on a parser or callback failure.
                        throw (IOException) JdbcUtils.sanitize(e, url);
                    } catch (RuntimeException e) {
                        Throwable sanitized = JdbcUtils.sanitize(e, url);
                        throw new JsonProviderException(sanitized.getMessage(), sanitized);
                    }
                }
            }) {
                JsonLines.execute(input, parameters, callback, url);
            } catch (IOException e) {
                throw new JsonProviderException("Cannot read JSON Lines input " + JdbcUtils.getUrlDescription(url),
                        JdbcUtils.sanitize(e, url));
            }
            counter.statements++;
            return;
        }
        try (PushbackReader input = new PushbackReader(newInputReader(), 1)) {
            int first = input.read();
            if (first != -1 && !(first == 0xFEFF && StandardCharsets.UTF_8.equals(encoding))) {
                input.unread(first);
            }
            try (JsonParser parser = FACTORY.createParser(input)) {
                new JsonQuery(parser, path, parameters, callback).execute();
            }
        } catch (IOException e) {
            throw new JsonProviderException("Cannot read JSON input " + JdbcUtils.getUrlDescription(url),
                    JdbcUtils.sanitize(e, url));
        }
        counter.statements++;
    }

    public void executeScript(Resource script, ParametersCallback parameters) {
        checkOpen();
        if (reading) {
            throw new JsonProviderException("Cannot read and write through the same JSON connection");
        }
        writing = true;
        if (finished || outputFailed) {
            throw new JsonProviderException("JSON output is finalized or failed");
        }
        if (isReadonly()) {
            return;
        }
        try {
            EtlCancelledException.checkEtlCancelled();
            String text;
            try (Reader content = script.open()) {
                text = IOUtils.toString(content);
            }
            // Compare actual content, never Resource identity (dynamic includes can change).
            // Keep only the latest template so memory cannot grow with invocation count.
            if (template == null || !template.matches(text)) {
                template = new JsonTemplate(text);
            }
            String record = JsonValues.serialize(template.evaluate(parameters));
            EtlCancelledException.checkEtlCancelled();
            try {
                if (output == null) {
                    output = newOutputWriter();
                    if (!jsonLines) {
                        output.write('[');
                    }
                } else if (!jsonLines) {
                    output.write(',');
                }
                output.write(record);
                if (jsonLines) {
                    output.write('\n');
                }
            } catch (IOException e) {
                outputFailed = true;
                throw e;
            }
            counter.statements++;
        } catch (IOException e) {
            outputFailed = true;
            throw outputError(e);
        } catch (RuntimeException e) {
            outputFailed = true;
            throw e;
        }
    }

    protected Writer newOutputWriter() throws IOException {
        return new OutputStreamWriter(url == null ? new FilterOutputStream(System.out) {
            @Override
            public void close() throws IOException {
                flush();
            }
        } : IOUtils.getOutputStream(url), encoding.newEncoder());
    }

    private JsonProviderException outputError(IOException cause) {
        return new JsonProviderException("Cannot write JSON output " + JdbcUtils.getUrlDescription(url)
                + " at record " + (counter.statements + (finished ? 0 : 1))
                + (finished ? " during finalization" : " during record emission"),
                JdbcUtils.sanitize(cause, url));
    }

    @Override
    public void commit() {
        checkOpen();
        if (outputFailed) {
            throw new JsonProviderException("JSON output failed; export is incomplete");
        }
        if (output != null && !finished) {
            finished = true;
            try {
                if (!jsonLines) {
                    output.write(']');
                }
                output.flush();
            } catch (IOException e) {
                outputFailed = true;
                try {
                    output.close();
                } catch (IOException cleanup) {
                    e.addSuppressed(cleanup);
                } finally {
                    output = null;
                }
                throw outputError(e);
            }
            try {
                output.close();
            } catch (IOException e) {
                outputFailed = true;
                throw outputError(e);
            } finally {
                output = null;
            }
        }
    }

    @Override
    public void rollback() {
        // Bytes cannot be undone. Prevent later finalization of an aborted export.
        if (writing) {
            outputFailed = true;
        }
    }

    private void checkOpen() {
        if (closed) {
            throw new JsonProviderException("JSON connection is closed");
        }
    }

    public void close() {
        closed = true;
        template = null;
        if (output != null) {
            try {
                output.close();
            } catch (IOException e) {
                throw outputError(e);
            } finally {
                output = null;
            }
        }
    }
}
