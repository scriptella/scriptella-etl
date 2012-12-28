/*
 * Copyright 2006-2012 The Scriptella Project Team.
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
package scriptella.driver.text;

import scriptella.core.EtlCancelledException;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ParametersCallback;
import scriptella.util.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Writes a text content to the output and performs properties expansion.
 * <p>This class is a simplified template engine similar to Velocity,
 * but of course less powerful. Nevertheless this executor
 * is generally faster than external template engines and does not require any additional libraries.
 * <p><u>Example:</u></p>
 * <b>Script:</b>
 * <code><pre>
 * $rownum;$name;$surname;${email.trim().replaceAll('@','_at_')}
 * </pre></code>
 * <b>Parameters:</b>
 * <table border=1>
 * <tr>
 * <th>rownum</th>
 * <th>name</th>
 * <th>surname</th>
 * <th>email</th>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>John</td>
 * <td>G</td>
 * <td>  john@nosuchhost.com</td>
 * </tr>
 * </table>
 * <b>Result:</b>
 * <code><pre>
 * 1;John;G;john_at_nosuchhost.com
 * </pre></code>
 *
 * @author Fyodor Kupolov
 * @version 1.0
 * @see PropertiesSubstitutor
 */
public class TextScriptExecutor implements Closeable, Flushable {
    private BufferedWriter out;
    private PropertiesSubstitutor ps;
    private TextConnectionParameters textParams;

    /**
     * Creates an executor.
     *
     * @param out        writer for output.
     * @param textParams text connection parameters.
     */
    public TextScriptExecutor(Writer out, TextConnectionParameters textParams) {
        ps = new PropertiesSubstitutor();
        this.out = IOUtils.asBuffered(out);
        this.textParams = textParams;
    }

    /**
     * Parses a script from read, expands properties and produces the output.
     *
     * @param reader  script content.
     * @param pc      parameters for substitution.
     * @param counter statements counter.
     */
    public void execute(Reader reader, ParametersCallback pc, AbstractConnection.StatementCounter counter) {
        ps.setParameters(textParams.getPropertyFormatter().format(pc));
        BufferedReader r = IOUtils.asBuffered(reader);
        final boolean trimLines = textParams.isTrimLines();
        try {
            for (String line; (line = r.readLine()) != null;) {
                EtlCancelledException.checkEtlCancelled();
                if (trimLines) {
                    line = line.trim();
                }
                //If trimming is disabled (keeping format) or if line is not empty 
                if (!trimLines || line.length() > 0) {
                    try {
                        out.write(ps.substitute(line));
                        out.write(textParams.getEol());
                        counter.statements++;
                    } catch (IOException e) {
                        throw new TextProviderException("Failed writing to a text file", e);
                    }
                }
            }
        } catch (IOException e) {
            throw new TextProviderException("Failed reading a script file", e);
        }
    }


    public void flush() throws IOException {
        out.flush();
    }

    public void close() throws IOException {
        IOUtils.closeSilently(out);
        out = null;
        ps = null;
    }
}
