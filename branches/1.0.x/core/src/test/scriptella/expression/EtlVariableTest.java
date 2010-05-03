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
package scriptella.expression;

import scriptella.AbstractTestCase;
import scriptella.spi.MockParametersCallbacks;

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
    /**
     * Simple tests.
     *
     * @throws ParseException if parsing fails.
     */
    public void test() throws ParseException {
        EtlVariable etl = EtlVariable.get();
        EtlVariable.DateUtils d = etl.getDate();
        assertTrue(d.now().getTime() >= System.currentTimeMillis());
        assertEquals("2007", d.format(d.parse("18.03.2007", "dd.MM.yyyy"), "yyyy"));
        //Tests if today method returns correct year 
        assertTrue(d.today().indexOf(d.now("yyyy"))>0);
    }

    /**
     * Test support of etl variable in expressions.
     */
    public void testExpression() {
        Date d = (Date) Expression.compile("etl.date.now()").evaluate(MockParametersCallbacks.NULL);
        assertTrue(d.getTime() >= System.currentTimeMillis());
        String format = "MMddyyyy";
        Map<String, Object> m = new HashMap<String, Object>();
        PropertiesSubstitutor ps = new PropertiesSubstitutor(MockParametersCallbacks.fromMap(m));
        String s = ps.substitute("${etl.date.today('" + format + "')}");
        assertEquals(new SimpleDateFormat(format).format(new Date()), s);
    }

    public void testText() {
        Object o = Expression.compile("etl.text.ifNull(a)").evaluate(MockParametersCallbacks.NULL);
        assertEquals("", o);
        o = Expression.compile("etl.text.ifNull(a,1)").evaluate(MockParametersCallbacks.NULL);
        assertEquals(1, o);
        o = Expression.compile("etl.text.ifNull(a,1)").evaluate(MockParametersCallbacks.fromMap(
                Collections.singletonMap("a", 2)));
        assertEquals(2, o);
        o = Expression.compile("etl.text.nullIf(a,1)").evaluate(MockParametersCallbacks.fromMap(
                Collections.singletonMap("a", 1)));
        assertNull(o);
        o = Expression.compile("etl.text.nullIf(a,1)").evaluate(MockParametersCallbacks.NULL);
        assertNull(o);
        o = Expression.compile("etl.text.nullIf(a,1)").evaluate(MockParametersCallbacks.fromMap(
                Collections.singletonMap("a", 2)));
        assertEquals(2, o);
    }
}
