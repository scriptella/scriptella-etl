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
package scriptella.driver.xpath;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import scriptella.AbstractTestCase;
import scriptella.configuration.StringResource;
import scriptella.spi.AbstractConnection;
import scriptella.spi.IndexedQueryCallback;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;
import scriptella.spi.Resource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * Tests for {@link XPathQueryExecutor}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class XPathQueryExecutorTest extends AbstractTestCase {
    private DocumentBuilder documentBuilder;
    private ThreadLocal<Node> context;

    protected void setUp() throws Exception {
        documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        context = new ThreadLocal<Node>();
    }

    public void test() throws ParserConfigurationException, IOException, SAXException {
        Document doc = documentBuilder.parse(getClass().getResourceAsStream("xml1.xml"));
        Resource res = new StringResource("/html/body/table/tr");
        XPathQueryExecutor exec = new XPathQueryExecutor(context, doc, res, new XPathExpressionCompiler(), new AbstractConnection.StatementCounter(), true);
        IndexedQueryCallback callback = new IndexedQueryCallback() {

            protected void processRow(final ParametersCallback parameters, final int rowNumber) {
                if (rowNumber == 0) {
                    assertEquals("red", parameters.getParameter("bgcolor"));
                    assertEquals("Column1", ((String[]) parameters.getParameter("th"))[0]);
                    assertEquals("Column2", ((String[]) parameters.getParameter("th"))[1]);
                } else {
                    assertEquals(String.valueOf(rowNumber * 2 - 1), ((String[]) parameters.getParameter("td"))[0]);
                    assertEquals(String.valueOf(rowNumber * 2), ((String[]) parameters.getParameter("td"))[1]);
                }
            }
        };
        exec.execute(callback, MockParametersCallbacks.NULL);
        assertEquals(3,callback.getRowsNumber());
    }

    public void test2() throws ParserConfigurationException, IOException, SAXException {
        Document doc = documentBuilder.parse(getClass().getResourceAsStream("xml2.xml"));
        Resource res = new StringResource("  /xml/element[@attribute=1]  | /xml/element[not(@attribute)]");
        XPathQueryExecutor exec = new XPathQueryExecutor(context, doc, res, new XPathExpressionCompiler(), new AbstractConnection.StatementCounter(), false);
        IndexedQueryCallback callback = new IndexedQueryCallback() {

            protected void processRow(final ParametersCallback parameters, final int rowNumber) {
                if (rowNumber == 0) {
                    assertEquals("1", parameters.getParameter("attribute"));
                } else {
                    assertEquals("", parameters.getParameter("element"));
                }
            }
        };
        exec.execute(callback, MockParametersCallbacks.NULL);
        assertEquals(2,callback.getRowsNumber());
        //Now select element2, also test substitution
        res = new StringResource(" /xml/$element2 ");
        exec = new XPathQueryExecutor(context, doc, res, new XPathExpressionCompiler(), new AbstractConnection.StatementCounter(), false);
        callback = new IndexedQueryCallback() {

            protected void processRow(final ParametersCallback parameters, final int rowNumber) {
                if (rowNumber == 0) {
                    assertEquals("1", parameters.getParameter("attribute"));
                } else {
                    assertEquals("el2", parameters.getParameter("element2"));
                }
            }
        };
        exec.execute(callback, MockParametersCallbacks.NAME);
        assertEquals(2,callback.getRowsNumber());
    }

}
