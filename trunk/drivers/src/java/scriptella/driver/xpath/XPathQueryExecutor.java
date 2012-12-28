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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.IOUtils;
import scriptella.util.StringUtils;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

/**
 * Executor for XPath queries.
 *
 * @author Fyodor Kupolov
 * @author Martin Alderson
 * @version 1.0
 */
public class XPathQueryExecutor implements ParametersCallback {
    private Node node;
    private PropertiesSubstitutor substitutor = new PropertiesSubstitutor();
    private Document document;
    private String expressionStr;
    private XPathExpressionCompiler compiler;
    private AbstractConnection.StatementCounter counter;
    private boolean returnArrays;
    ThreadLocal<Node> context;

    /**
     * Crates executor to query document using a specified xpath expression.
     *
     * @param context       thread local for sharing current node between queries.
     *                      The instance of thread local is shared between all connection queries.
     * @param document      document to query.
     * @param xpathResource resource with xpath expression.
     * @param compiler      xpath expression compiler
     * @param counter       statement counter.
     * @param returnArrays  true if string arrays should be returned for variables.
     */
    public XPathQueryExecutor(ThreadLocal<Node> context, Document document, Resource xpathResource, XPathExpressionCompiler compiler, AbstractConnection.StatementCounter counter, boolean returnArrays) {
        this.context = context;
        this.document = document;
        this.compiler = compiler;
        this.counter = counter;
        this.returnArrays = returnArrays;
        try {
            expressionStr = IOUtils.toString(xpathResource.open());
        } catch (IOException e) {
            throw new XPathProviderException("Unable to read XPath query content");
        }
    }

    /**
     * Executes a query and notifies queryCallback for each found node.
     *
     * @param queryCallback    callback to notify for each found node.
     * @param parentParameters parent parameters to inherit.
     */
    public void execute(final QueryCallback queryCallback, final ParametersCallback parentParameters) {
        // Set the context node to the selected node of the nearest xpath query of this connection.
        final Node contextNode = context.get();
        try {
            substitutor.setParameters(parentParameters);
            XPathExpression xpathExpression = compiler.compile(substitutor.substitute(expressionStr));
            NodeList nList = (NodeList) xpathExpression.evaluate(
                    contextNode == null ? document : contextNode, XPathConstants.NODESET);
            counter.statements++;

            int n = nList.getLength();
            for (int i = 0; i < n; i++) {
                node = nList.item(i);
                context.set(node); //store the context local to the current thread
                queryCallback.processRow(this);
            }
        } catch (XPathExpressionException e) {
            throw new XPathProviderException("Failed to evaluate XPath query", e);
        } finally {
            substitutor.setParameters(null);
            context.set(contextNode); //restore ThreadLocal state
        }
    }

    public Object getParameter(final String name) {
        Object result = null;
        
        if (name.equals("node")) {
            // A helper object.
            return new NodeVariable(compiler, node);
        }
        
        if (node instanceof Element) { //if element
            //Now we use a trick to determine if node contains "name" attribute
            //element.getAttribute returns "" for declared and declared attributes
            Node item = node.getAttributes().getNamedItem(name);
            result = item == null ? null : StringUtils.nullsafeTrim(item.getNodeValue()); //Get attribute value for name
        }

        if (result == null) {
            // Try to retrieve the text value(s) of the immediate child element(s) with the specified name
            NodeVariable nodeVariable = new NodeVariable(compiler, node);
            if (returnArrays) {
                result = nodeVariable.getStringArray("./" + name);
            } else {
                result = nodeVariable.getString("./" + name);
            }
        }

        //If previous check was unsuccessful and the selected node has specified name
        if (result == null && name.equals(node.getNodeName())) {
            result = StringUtils.nullsafeTrim(node.getTextContent()); //returns its text content
        }
        
        //if result=null fallback to parent parameters
        return result == null ? substitutor.getParameters().getParameter(name) : result;
    }
}
