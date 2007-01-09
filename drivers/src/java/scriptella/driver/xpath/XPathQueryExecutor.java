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
package scriptella.driver.xpath;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.IOUtils;
import scriptella.util.StringUtils;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Executor for XPath queries.
 */
public class XPathQueryExecutor implements ParametersCallback {
    private Node node;
    private NamedNodeMap attributes;
    private ParametersCallback parentParameters;
    private Document document;
    private Collection<XPathExpression> expressions;
    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();


    /**
     * Crates executor to query document using a specified xpath expression.
     *
     * @param document      document to query.
     * @param xpathResource resource with xpath expression.
     */
    public XPathQueryExecutor(Document document, Resource xpathResource) {
        this.document = document;
        try {
            BufferedReader reader = IOUtils.asBuffered(xpathResource.open());
            XPath xPath = XPATH_FACTORY.newXPath();
            expressions = new ArrayList<XPathExpression>();
            for (String line; (line = reader.readLine()) != null;) {
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        expressions.add(xPath.compile(line));
                    } catch (XPathExpressionException e) {
                        throw new XPathProviderException("Unable to compile XPath query: " + line, e);
                    }
                }
            }
        } catch (IOException e) {
            throw new XPathProviderException("Unable to read XPath query content");
        }
        if (expressions.isEmpty()) {
            throw new XPathProviderException("XPath query expected");
        }
    }

    /**
     * Executes a query and notifies queryCallback for each found node.
     *
     * @param queryCallback    callback to notify for each found node.
     * @param parentParameters parent parameters to inherit.
     */
    public void execute(final QueryCallback queryCallback, final ParametersCallback parentParameters) {
        try {
            this.parentParameters = parentParameters;
            for (XPathExpression xpath : expressions) {
                NodeIterator itt = (NodeIterator) xpath.evaluate(document, XPathConstants.NODESET);
                for (; (node = itt.nextNode()) != null;) {
                    attributes = node.getAttributes();
                    queryCallback.processRow(this);
                }
            }
        } catch (XPathExpressionException e) {
            throw new XPathProviderException("Failed to evaluate XPath query", e);
        } finally {
            this.parentParameters = null;
        }
    }

    public Object getParameter(final String name) {
        Object result = null;
        //If name is a number and node has attributes - try to get an attribute by index
        if (attributes != null && StringUtils.isDecimalInt(name)) {
            result = attributes.item(Integer.parseInt(name));
        } else if ("row".equals(name)) {
            result = node; //$row returns entire node
        } else if (attributes != null) {
            result = attributes.getNamedItem(name); //Get attribute value for name
        }
        //if result=null fallback to parent parameters
        return result == null ? parentParameters.getParameter(name) : result;
    }
}
