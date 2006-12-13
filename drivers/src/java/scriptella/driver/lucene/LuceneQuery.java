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
package scriptella.driver.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.util.ExceptionUtils;
import scriptella.util.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Query for Lucene indexed data.
 *
 * @author Kirill Volgin
 * @version 1.0
 */
public class LuceneQuery implements ParametersCallback, Closeable {

    private static final Logger LOG = Logger.getLogger(LuceneQuery.class.getName());
    private PropertiesSubstitutor substitutor = new PropertiesSubstitutor();
    private String indexPath;
    private Document result;
    private QueryCallback queryCallback;

    /**
     * Creates a query for Lucene index.
     *
     * @param indexPath path to lucene index
     * @param parametersCallback parameters to use.
     * @param queryCallback callback to use for result set iteration.
     */
    public LuceneQuery(String indexPath, ParametersCallback parametersCallback, QueryCallback queryCallback) {
        this.indexPath = indexPath;
        substitutor = new PropertiesSubstitutor(parametersCallback);
        this.queryCallback = queryCallback;
    }

    /**
     * Executes a query.
     * @param queryReader query content reader. Closed after this method completes.
     * @throws java.io.IOException if IO error occurs.
     */
    public void execute(Reader queryReader, final Collection<String> fields) {
        IndexReader ir = null;
        Searcher searcher = null;
        try {
            try {
                ir = IndexReader.open(indexPath);
            } catch (IOException e) {
                throw new LuceneProviderException("Failed to open index " + indexPath, e);
            }
            searcher = new IndexSearcher(ir);
            final Analyzer analyzer = new StandardAnalyzer();
            String queryContent;
            try {
                queryContent = IOUtils.toString(queryReader);
            } catch (IOException e) {
                throw new LuceneProviderException("Failed to load query content.", e);
            }
            for (String field : fields) {
                QueryParser parser = new QueryParser(field, analyzer);
                Query query = parser.parse(queryContent);
                try {
                    iterate(searcher.search(query));
                } catch (IOException e) {
                    throw new LuceneProviderException("Failed to search query.", e);
                }
            }
        } catch (ParseException e) {
            throw new LuceneProviderException("Failed to parse query.", e);
        } finally { //clean up
            IOUtils.closeSilently(queryReader);
            try {
                ir.close();
            } catch (Exception e) {
                ExceptionUtils.ignoreThrowable(e);
            }
            try {
                searcher.close();
            } catch (Exception e) {
                ExceptionUtils.ignoreThrowable(e);
            }

        }

    }

    /**
     * Iterates search result
     * @param hits a ranked list of found documents
     */
    void iterate(Hits hits) {
        for (int i = 0; i < hits.length(); i++) {

            try {
                result = hits.doc(i);
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Processing search result: " + result);
                }
                queryCallback.processRow(this); //notifying a callback
            } catch (IOException e) {
                throw new LuceneProviderException("Failed to get query result.",e);
            }
        }
    }

    public Object getParameter(final String name) {
        String res = result.get(name);
        if (res != null) {
            return res;
        }
        return substitutor.getParameters().getParameter(name);
    }


    public void close() throws IOException {
        substitutor = null;
        queryCallback = null;
        result = null;
    }
}
