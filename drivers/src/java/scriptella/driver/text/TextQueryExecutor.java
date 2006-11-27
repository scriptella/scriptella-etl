/*
 * Copyright 2006 The Scriptella Project Team.
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

import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.util.ExceptionUtils;
import scriptella.util.IOUtils;
import scriptella.util.StringUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class executes a regex query over a text file content.
 * <p>The query has a standard regex syntax.
 * Matching is performed for each line of the text file.
 * <p><u>Example:</u></p>
 * <b>Query:</b> <code>.*;(2\d+);.*</code><br>
 * <b>Text File:</b>
 * <code>
 * <pre>
 * 1;100;record 1
 * 2;200;record 2
 * 3;250;record 3
 * </pre>
 * </code>
 * As the result of the query execution the following result set is produced:
 * <table border=1>
 *   <tr>
 *     <th>Column name/<br>row number</th>
 *     <th>0</th>
 *     <th>1</th>
 *   </tr>
 *   <tr>
 *     <td>1</td>
 *     <td>2;200;record 2</td>
 *     <td>200</td>
 *   </tr>
 *   <tr>
 *     <td>2</td>
 *     <td>3;250;record 3</td>
 *     <td>250</td>
 *   </tr>
 * </table>
 * Where column name corresponds to the matched regex group name.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class TextQueryExecutor implements ParametersCallback, Closeable {
    private static final Logger LOG = Logger.getLogger(TextQueryExecutor.class.getName());
    private final ParametersCallback params;
    private final Pattern query;
    private BufferedReader reader;
    private Matcher result;

    public TextQueryExecutor(final String query, final Reader in, final ParametersCallback parentParametersCallback) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        this.params = parentParametersCallback;
        try {
            this.query = Pattern.compile(new PropertiesSubstitutor(parentParametersCallback).substitute(query), Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            throw new TextProviderException("Cannot parse query " + query, e);
        }
        reader = IOUtils.asBuffered(in);

    }

    /**
     * Executes a query and iterates the resultset using the callback.
     *
     * @param qc callback to notify on each row.
     */
    public void execute(final QueryCallback qc) {
        Matcher m = null;

        try {
            for (String line;(line=reader.readLine()) != null;) {
                if (m==null) {
                    m=query.matcher(line);
                } else {
                    m.reset(line);
                }
                if (m.matches()) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.info("Pattern matched: " + m);
                    }
                    result=m;
                    qc.processRow(this);
                }
            }
        } catch (IOException e) {
            throw new TextProviderException("Unable to parse text file", e);
        }
    }


    /**
     * Returns the value of the named parameter.
     * <p>Use index of the captured group to obtain the value of the matched substring.
     *
     * @param name parameter name.
     * @return parameter value.
     */
    public Object getParameter(final String name) {
        if (StringUtils.isDecimalInt(name)) {
            try {
                int ind = Integer.parseInt(name);
                if (ind >= 0 && ind <= result.groupCount()) {
                    return result.group(ind);
                }
            } catch (NumberFormatException e) {
                ExceptionUtils.ignoreThrowable(e);
            }
        }
        return params.getParameter(name);
    }

    public void close() throws IOException {
        IOUtils.closeSilently(reader);
        reader=null;
    }
}
