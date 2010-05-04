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

import scriptella.util.LRUMap;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.Map;

/**
 * Represents a facade for compiling xpath expressions.
 * <p>Expressions caching is also supported.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class XPathExpressionCompiler {
    private static final int DEFAULT_SIZE = 100;
    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();
    private Map<String, XPathExpression> cache = new LRUMap<String, XPathExpression>(DEFAULT_SIZE);

    public XPathExpression compile(final String expression) {
        String trimmedEx = expression.trim();
        XPathExpression ex = cache.get(trimmedEx);
        if (ex == null) {
            try {
                cache.put(trimmedEx, ex = XPATH_FACTORY.newXPath().compile(trimmedEx));
            } catch (XPathExpressionException e) {
                throw new XPathProviderException("Unable to compile XPath query: " + trimmedEx, e);
            }
        }
        return ex;
    }
}
