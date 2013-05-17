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
package scriptella.core;

import scriptella.AbstractTestCase;
import scriptella.execution.EtlContext;
import scriptella.expression.Expression;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlVariableTest extends AbstractTestCase {
    private static ParametersCallback params = MockParametersCallbacks.fromMap(
            Collections.singletonMap("etl", new EtlVariable(MockParametersCallbacks.SIMPLE, null)));

    /**
     * Simple tests.
     *
     * @throws ParseException if parsing fails.
     */
    public void test() throws ParseException {
        EtlVariable.DateUtils d = new EtlVariable().getDate();
        assertTrue(d.now().getTime() >= System.currentTimeMillis());
        assertEquals("2007", d.format(d.parse("18.03.2007", "dd.MM.yyyy"), "yyyy"));
        //Tests if today method returns correct year 
        assertTrue(d.today().indexOf(d.now("yyyy")) > 0);
    }

    /**
     * Test support of etl variable in expressions.
     */
    public void testExpression() {
        Date d = (Date) Expression.compile("etl.date.now()").evaluate(params);
        assertTrue(d.getTime() >= System.currentTimeMillis());
        String format = "MMddyyyy";

        PropertiesSubstitutor ps = new PropertiesSubstitutor(params);
        String s = ps.substitute("${etl.date.today('" + format + "')}");
        assertEquals(new SimpleDateFormat(format).format(new Date()), s);
    }

    public void testText() {
        Object o = Expression.compile("etl.text.ifNull(a)").evaluate(params);
        assertEquals("", o);
        o = Expression.compile("etl.text.ifNull(a,1)").evaluate(params);
        assertEquals(1, o);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("a", 2);
        map.put(EtlVariable.NAME, new EtlVariable());
        ParametersCallback parametersCallback = MockParametersCallbacks.fromMap(map);

        o = Expression.compile("etl.text.ifNull(a,1)").evaluate(parametersCallback);
        assertEquals(2, o);

        map.put("a", 1);
        o = Expression.compile("etl.text.nullIf(a,1)").evaluate(parametersCallback);
        assertNull(o);

        map.remove("a");
        o = Expression.compile("etl.text.nullIf(a,1)").evaluate(parametersCallback);
        assertNull(o);

        map.put("a", 2);
        o = Expression.compile("etl.text.nullIf(a,1)").evaluate(parametersCallback);
        assertEquals(2, o);
    }

    public void testNewSyntax() {
        Object o = Expression.compile("text:ifNull(a)").evaluate(params);
        assertEquals("", o);
        Date d = (Date) Expression.compile("date:now()").evaluate(params);
        assertTrue(d.getTime() >= System.currentTimeMillis());
        String p = (String) Expression.compile("class:forName('java.lang.System').getProperty('java.version')").
                evaluate(params);
        assertTrue(p != null);
    }

    public void testGetParameter() {
        EtlVariable etlVariable = new EtlVariable(MockParametersCallbacks.NAME, null);
        assertEquals("var name", etlVariable.getParameter("var name"));
    }

    public void testGetGlobalVars() {
        EtlVariable etlVariable = new EtlVariable(MockParametersCallbacks.NULL, new EtlContext());
        etlVariable.getGlobals().put("g", "1");
        assertEquals("1", etlVariable.getGlobals().get("g"));
    }


}
