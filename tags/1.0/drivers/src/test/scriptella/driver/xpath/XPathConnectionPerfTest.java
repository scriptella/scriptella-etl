/*
 * Copyright 2006-2009 The Scriptella Project Team.
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
import org.w3c.dom.Element;
import scriptella.AbstractTestCase;
import scriptella.configuration.MockConnectionEl;
import scriptella.configuration.StringResource;
import scriptella.spi.AbstractConnection;
import scriptella.spi.Connection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.IndexedQueryCallback;
import scriptella.spi.MockDriverContext;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;

import javax.xml.parsers.ParserConfigurationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link scriptella.driver.xpath.XPathConnection}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class XPathConnectionPerfTest extends AbstractTestCase {
    /**
     * History:
     * 19.01.2007 - Duron 1.7Mhz - 1125 ms
     */
    public void testQuery() {
        //Create a configuration with non default values
        Map<String, String> props = new HashMap<String, String>();
        ConnectionParameters cp = new ConnectionParameters(new MockConnectionEl(props, getClass().getResource("excel.xml").toString()), MockDriverContext.INSTANCE);

        Connection con = new Driver().connect(cp);
        IndexedQueryCallback queryCallback = new IndexedQueryCallback() {
            protected void processRow(final ParametersCallback parameters, final int rowNumber) {
                parameters.getParameter("Cell");
            }
        };
        //Quering 200 times.
        for (int i = 0; i < 200; i++) {
            con.executeQuery(new StringResource("/$Workbook/Worksheet/Table/Row"), MockParametersCallbacks.NAME, queryCallback);
        }
        assertEquals(600, queryCallback.getRowsNumber());
    }

    /**
     * History:
     * 19.01.2007 - Duron 1.7Mhz - 1220 ms
     */
    public void testQueryLargeDOM() throws ParserConfigurationException {
        //Create a configuration with non default values
        Document doc = XPathConnection.DBF.newDocumentBuilder().newDocument();
        Element root = doc.createElement("table");
        doc.appendChild(root);
        for (int i = 0; i < 4000; i++) {
            Element row = doc.createElement("row");
            row.setAttribute("id", String.valueOf(i));
            root.appendChild(row);
        }
        //Quering 200 times.
        XPathQueryExecutor qe = new XPathQueryExecutor(doc, new StringResource("/table/row[@id mod 2=0]"), new XPathExpressionCompiler(), new AbstractConnection.StatementCounter());
        for (int i = 0; i < 20; i++) {
            IndexedQueryCallback queryCallback = new IndexedQueryCallback() {
                protected void processRow(final ParametersCallback parameters, final int rowNumber) {
                    assertTrue(Integer.parseInt((String) parameters.getParameter("id")) % 2 == 0);
                }
            };
            qe.execute(queryCallback, MockParametersCallbacks.NULL);
            assertEquals(2000, queryCallback.getRowsNumber());
        }


    }


}
