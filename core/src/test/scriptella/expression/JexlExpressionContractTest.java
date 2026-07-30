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
package scriptella.expression;

import scriptella.AbstractTestCase;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Characterization of the JEXL expression contracts used by Scriptella.
 * <p>Chunk 5A/5B dependency upgrades must keep these green. The JEXL 2.1.1
 * candidate was rejected because it treats {@code var} (and {@code return}) as
 * reserved words, breaking identifier use of {@code var} proven below.
 *
 * @author Scriptella Project Team
 */
public class JexlExpressionContractTest extends AbstractTestCase {

    /**
     * Pins that {@code var} remains a legal identifier (JEXL 2.0.1).
     * JEXL 2.1+ reserves {@code var} for local declarations and fails parse.
     */
    public void testParameterLookupAndConcatenation() {
        Object value = Expression.compile("file + var").evaluate(MockParametersCallbacks.SIMPLE);
        assertEquals("*file**var*", value);
    }

    public void testDottedParameterNamesAreFlatKeys() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("url.prefix", "jdbc:h2:mem");
        map.put("dbname", "contract");
        ParametersCallback cb = MockParametersCallbacks.fromMap(map);
        // $name form allows dotted flat keys (PropertiesTest url.prefix pattern).
        PropertiesSubstitutor ps = new PropertiesSubstitutor(cb);
        assertEquals("jdbc:h2:mem:contract", ps.substitute("$url.prefix:$dbname"));
    }

    public void testMissingParameterIsSilent() {
        // has() always returns true; missing parameters evaluate without JEXL warnings.
        Object value = Expression.compile("missingVar").evaluate(MockParametersCallbacks.NULL);
        assertNull(value);
    }

    public void testFunctionNamespaces() {
        Object empty = Expression.compile("text:ifNull(a)").evaluate(MockParametersCallbacks.NULL);
        assertEquals("", empty);
        Object now = Expression.compile("date:now()").evaluate(MockParametersCallbacks.NULL);
        assertTrue(now instanceof Date);
        String version = (String) Expression.compile(
                "class:forName('java.lang.System').getProperty('java.version')")
                .evaluate(MockParametersCallbacks.NULL);
        assertNotNull(version);
        assertTrue(version.length() > 0);
    }

    public void testContextSetIsRejected() {
        // Direct engine path used by ${} expressions rejects context mutation.
        org.apache.commons.jexl2.JexlEngine engine = JexlExpression.newJexlEngine();
        org.apache.commons.jexl2.Expression expression = engine.createExpression("1+1");
        org.apache.commons.jexl2.JexlContext ctx = new org.apache.commons.jexl2.JexlContext() {
            public Object get(String name) {
                return null;
            }

            public void set(String name, Object value) {
                throw new UnsupportedOperationException(
                        "Setting variables in ${} JEXL expression is not allowed");
            }

            public boolean has(String name) {
                return true;
            }
        };
        assertEquals(2, ((Number) expression.evaluate(ctx)).intValue());
        try {
            ctx.set("x", 1);
            fail("Expression context must reject set()");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("not allowed"));
        }
    }
}
