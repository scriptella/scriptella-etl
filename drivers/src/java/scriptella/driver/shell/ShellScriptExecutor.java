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

import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ParametersCallback;
import scriptella.util.IOUtils;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.ExecutionException;

/**
 * Executes a shell script and sends its output to the specified writer.
 * <p> See scriptella/driver/shell/ShellDriverITest.xml for usage example</p>
 *
 * @author Fyodor Kupolov
 * @version 1.0
 * @see PropertiesSubstitutor
 */
public class ShellScriptExecutor implements Closeable, Flushable {
    private static final int MAX_LENGTH = 100000; // Script body cannot be larger than 100Kb
    private BufferedWriter out;
    private PropertiesSubstitutor ps;
    private ShellConnectionParameters shellParams;
    private final ShellCommandRunner shellCommandRunner;

    /**
     * Creates an executor.
     *
     * @param out        writer for output.
     * @param params     connection parameters.
     */
    public ShellScriptExecutor(Writer out, ShellConnectionParameters params) {
        ps = new PropertiesSubstitutor();
        this.out = IOUtils.asBuffered(out);
        this.shellParams = params;
        shellCommandRunner = new ShellCommandRunner(shellParams.getShellCommandArgs(), this.out);
    }

    /**
     * Parses a script from read, expands properties and produces the output.
     *
     * @param reader  script content.
     * @param pc      parameters for substitution.
     * @param counter statements counter.
     */
    public void execute(Reader reader, ParametersCallback pc, AbstractConnection.StatementCounter counter) {
        ps.setParameters(shellParams.getPropertyFormatter().format(pc));
        String scriptText;
        try {
            scriptText = IOUtils.toString(reader, MAX_LENGTH);
        } catch (IOException e) {
            throw new ShellProviderException("Failed reading a script file", e);
        }
        scriptText = ps.substitute(scriptText);
        try {
            shellCommandRunner.exec(scriptText);
        } catch (IOException e) {
            throw new ShellProviderException("Failed executing a script", e);
        }

        try {
            shellCommandRunner.waitForAndCheckExceptions();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ShellProviderException("Interrupted while waiting for the script to finish", e);
        } catch (ExecutionException e) {
            throw new ShellProviderException(e.getMessage(), e);
        }
        counter.statements++;
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void close() {
        IOUtils.closeSilently(out);
        shellCommandRunner.close();
        out = null;
        ps = null;
    }
}
