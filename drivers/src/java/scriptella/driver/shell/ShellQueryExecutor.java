/*
 * Copyright 2006-2010 The Scriptella Project Team.
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
import scriptella.spi.QueryCallback;
import scriptella.util.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.util.logging.Logger;

/**
 * This class executes a shell script from the body of the query and exports its output as a set of rows,
 * each row representing a line in the output.
 * <p>
 * Rows can be referenced by the {@code row} variable.
 *

 * @author Fyodor Kupolov
 * @version 1.0
 */
class ShellQueryExecutor implements ParametersCallback {
    private static final Logger LOG = Logger.getLogger(ShellQueryExecutor.class.getName());
    private static final int MAX_LENGTH = 100000; // 100Kb max query size

    private final PropertiesSubstitutor ps;
    private String query;
    private ShellConnectionParameters shellParams;
    private ShellCommandRunner shellCommandRunner;
    private final BufferedWriter shellCommandOutWriter;
    private final BufferedReader bufferedReader;
    private String currentRow;

    public ShellQueryExecutor(final Reader queryReader, final PropertiesSubstitutor substitutor,
                             final ShellConnectionParameters shellParams) {
        this.shellParams = shellParams;
        ps = substitutor;
        String queryStr;
        try {
            queryStr = IOUtils.toString(queryReader, MAX_LENGTH);
        } catch (IOException e) {
            throw new ShellProviderException("Unable to read query content", e);
        }
        query = ps.substitute(queryStr);
        PipedReader pipedReader = new PipedReader();
        bufferedReader = new BufferedReader(pipedReader);
        PipedWriter pipedWriter;
        try {
            pipedWriter = new PipedWriter(pipedReader);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        shellCommandOutWriter = new BufferedWriter(pipedWriter);
        shellCommandRunner = new ShellCommandRunner(shellParams.getShellCommandArgs(), shellCommandOutWriter);
    }

    /**
     * Executes a query and iterates the resultset using the callback.
     *
     * @param qc      callback to notify on each row.
     * @param counter statements counter.
     */
    public void execute(final QueryCallback qc, AbstractConnection.StatementCounter counter) {
        try {
            shellCommandRunner.exec(query);
        } catch (IOException e) {
            throw new ShellProviderException("Unable to execute the query", e);
        }
        // Close the writer after stdin has been consumed, so that bufferedReader will return null
        shellCommandRunner.executeAfterStdoutStderrConsumed(() -> IOUtils.closeSilently(shellCommandOutWriter));

        try {
            while ((currentRow = bufferedReader.readLine()) != null) {
                qc.processRow(this);
                counter.statements ++;
            }
        } catch (IOException e) {
            throw new ShellProviderException("Unable to read from shell script output", e);
        }
    }


    /**
     * Returns the value of the named parameter.
     * <p>Shell query only supports "row" as a reference to the current row, otherwise falls back to the parent parameters
     */
    public Object getParameter(final String name) {
        if ("row".equals(name)) {
           return currentRow;
        }
        return ps.getParameters().getParameter(name);
    }
}
