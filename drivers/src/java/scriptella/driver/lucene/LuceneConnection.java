/*
 * Copyright 2006-2007 The Scriptella Project Team.
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
package scriptella.driver.lucene;

import org.apache.lucene.index.IndexReader;
import scriptella.driver.text.TextProviderException;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a connection to a Lucene index.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Kirill Volgin
 * @version 1.0
 */
public class LuceneConnection extends AbstractConnection {

    private static final String[] EMPTY_ARRAY = new String[] {};

    private URL url;
    private Set<String> fields;
    private static final String DEFAULT_FIELD = "contents";

    /**
     * Instantiates a new connection to Lucene Query.
     * @param parameters connection parameters.
     */
    public LuceneConnection(ConnectionParameters parameters) {
        super(Driver.DIALECT_IDENTIFIER, parameters);
        url = parameters.getResolvedUrl();
        parseFields((String)parameters.getProperty("fields"));
    }



    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ProviderException {
        throw new UnsupportedOperationException("Script execution is not supported yet");
    }

    /**
     * Executes a query specified by its content.
     * <p/>
     *
     * @param queryContent       query content.
     * @param parametersCallback callback to get parameter values.
     * @param queryCallback      callback to call for each result set element produced by this query.
     * @see #executeScript(scriptella.spi.Resource, scriptella.spi.ParametersCallback)
     */
    public synchronized void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException{
        LuceneQuery query = null;
        Reader r;
        try {
            r = queryContent.open();
        } catch (IOException e) {
            throw new TextProviderException("Cannot open a query for reading", e);
        }
        try {
            query = new LuceneQuery(url.getFile(), parametersCallback, queryCallback);
            query.execute(r, fields);
        } finally {
            IOUtils.closeSilently(query);
        }
    }

    /**
     * Closes the connection and releases all related resources.
     */
    public void close() throws ProviderException {
    }

    /**
     * Parses given string to find default Lucene fields for search query
     * @param s given string
     */
    private void parseFields(String s) {
        if (s == null) {
            fields = new TreeSet<String>();
            fields.add(DEFAULT_FIELD);
        } else {
            String[] strings = (' '+s+' ').split(",");
            fields = new TreeSet<String>();
            for (int i = 0; i < strings.length; i++) {
                strings[i]=strings[i].trim();
                if ("".equals(strings[i])) {
                    fields.add(DEFAULT_FIELD); //default value
                } else if ("*".contains(strings[i])) {
                    try {
                        IndexReader ir = IndexReader.open(url.getFile());
                        fields.addAll(ir.getFieldNames(IndexReader.FieldOption.INDEXED));
                    } catch (IOException e) {
                        throw new LuceneProviderException("Failed to open lucene index.",e);
                    }
                } else {
                    fields.add(strings[i]);
                }

            }
        }
    }

}
