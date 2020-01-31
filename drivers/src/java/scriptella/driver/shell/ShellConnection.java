/*
 * Copyright 2006-2020 The Scriptella Project Team.
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
package scriptella.driver.shell;

import scriptella.driver.text.AbstractTextConnection;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.IOUtils;

import java.io.IOException;
import java.io.Reader;

/**
 * Represents a shell script connection.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ShellConnection extends AbstractTextConnection {
    private ShellScriptExecutor out;

    public ShellConnection(ShellConnectionParameters parameters) {
        super(parameters.getDialectIdentifier(), parameters);
    }

    public void executeScript(final Resource scriptContent, final ParametersCallback parametersCallback) throws ProviderException {
        initScriptExecutor();
        Reader reader = null;
        try {
            reader = scriptContent.open();
            out.execute(reader, parametersCallback, counter);
            if (getConnectionParameters().isFlush()) {
                out.flush();
            }
        } catch (IOException e) {
            throw new ShellProviderException("Failed to produce output", e);
        } finally {
            IOUtils.closeSilently(reader);
        }
    }

    /**
     * Lazily initializes script writer.
     */
    protected void initScriptExecutor() {
        if (out == null) {
            try {
                this.out = new ShellScriptExecutor(newOutputWriter(), getConnectionParameters());
            } catch (IOException e) {
                throw new ShellProviderException("Unable to open file " + getConnectionParameters().getUrl() + " for writing", e);
            }
        }
    }

    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException {
        Reader q;
        try {
            q = queryContent.open();
        } catch (IOException e) {
            throw new ShellProviderException("Cannot read a shell query", e);
        }

        try {
            new ShellQueryExecutor(q, new PropertiesSubstitutor(parametersCallback), getConnectionParameters()).
                    execute(queryCallback, counter);
        } finally {
            IOUtils.closeSilently(q);
        }
    }

    public void close() throws ProviderException {
        IOUtils.closeSilently(out);
        out = null;
    }

    @Override
    protected ShellConnectionParameters getConnectionParameters() {
        return (ShellConnectionParameters) super.getConnectionParameters();
    }
}
