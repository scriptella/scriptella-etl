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
package scriptella.driver.xpath;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import scriptella.util.StringUtils;

/**
 * A special <code>node</code> variable for sub-queries and scripts that wraps
 * the node selected by an XPath query, exposing useful methods.
 * @author Martin Alderson
 */
public class NodeVariable {
    public static final String NAME = "node";
    
    private XPathExpressionCompiler compiler;
    private Node node;
    
    
    public NodeVariable(XPathExpressionCompiler compiler, Node node) {
        this.compiler = compiler;
        this.node = node;
    }
    
    /**
     * Gets the text values of the nodes selected by the specified XPath expression.
     *
     * @param expression xpath expression to evaluate.
     * @return a String array of text values of the selected nodes.
     */
    public String[] getStringArray(String expression) {
        try {
            XPathExpression xpathExpression = compiler.compile(expression);
            NodeList nList = (NodeList) xpathExpression.evaluate(node, XPathConstants.NODESET);
            int n = nList.getLength();
            //Convert these elements to text and return a string array
            if (n > 0) {
                String[] r = new String[n];
                for (int i = 0; i < n; i++) {
                    r[i] = StringUtils.nullsafeTrim(nList.item(i).getTextContent());
                }
                return r;
            } else {
                return new String[0];
            }
        } catch (XPathExpressionException e) {
            throw new XPathProviderException("Failed to evaluate XPath query", e);
        }
    }
    
    /**
     * Gets the string value of the first selected node by the specified XPath expression.
     *
     * @param expression xpath expression to evaluate.
     * @return the text value of the first selected node, or null.
     */
    public String getString(String expression) {
        return getString(expression, null);
    }
    
    /**
     * Gets the string value of the first selected node by the specified XPath expression.
     *
     * @param expression xpath expression to evaluate.
     * @param ifNull String to return if the specified expression evaluates to null.
     * @return the text value of the first selected node, or the specified string if null.
     */
    public String getString(String expression, String ifNull) {
        try {
            XPathExpression xpathExpression = compiler.compile(expression);
            Node foundNode = (Node) xpathExpression.evaluate(node, XPathConstants.NODE);
            if (foundNode == null) {
                return ifNull;
            } else {
                return StringUtils.nullsafeTrim(foundNode.getTextContent());
            }
        } catch (XPathExpressionException e) {
            throw new XPathProviderException("Failed to evaluate XPath query", e);
        }
    }

    /**
     * Gets the text value(s) of the node(s) selected by the specified XPath expression.
     * 
     * @param expression xpath expression to evaluate.
     * @return a String array of the selected nodes, or just a single String if one node found.
     */
    public Object get(String expression) {
        String[] strings = getStringArray(expression);
        
        if (strings == null || strings.length < 1) {
            return null;
        } else if (strings.length == 1) {
            return strings[0];
        } else {
            return strings;
        }
    }

    /**
     * Removes this node from the document.  This is especially useful to improve
     * performance and reduce memory usage when processing large documents.
     */
    public void remove()
    {
        Node parentNode = node.getParentNode();
        if (parentNode == null) {
            parentNode = node.getOwnerDocument();
        }
        parentNode.removeChild(node);
    }
}
