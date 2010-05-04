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

import java.io.IOException;
import java.util.Collections;

/**
 * Tests {@link PropertiesSubstitutor}
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class PropertiesSubstitutorTest extends AbstractTestCase {
    public void testVerbatimString() {
        PropertiesSubstitutor ps = new PropertiesSubstitutor();
        String exp = "No $ Params to substitute$$$";
        ps.setParameters(MockParametersCallbacks.UNSUPPORTED);
        String s = ps.substitute(exp);
        assertEquals(exp, s);
        exp = "No Params to substitute";
        s = ps.substitute(exp);
        assertEquals(exp, s);
    }

    public void test() throws IOException {
        PropertiesSubstitutor ps = new PropertiesSubstitutor(MockParametersCallbacks.SIMPLE);
        String expression = "$$ Text${subst1}${subst2}$subst3$subst4 End of test";
        String s = ps.substitute(expression);
        String expected = "$$ Text*subst1**subst2**subst3**subst4* End of test";
        assertEquals(expected, s);
    }

    public void testNullProperties() throws IOException {
        PropertiesSubstitutor ps = new PropertiesSubstitutor(MockParametersCallbacks.NULL);
        String exp = "$$ Text${subst1}${subst2}$subst3$subst4 End of test";
        String s = ps.substitute(exp);
        assertEquals(exp, s);

        //Now check if nullString is set
        ps = new PropertiesSubstitutor(MockParametersCallbacks.NULL);
        ps.setNullString("NULL");
        assertEquals("NULL", ps.getNullString());
        String expr = "$$ Test ${ex} ex ${ex1+ex2}....$test";
        exp = "$$ Test NULL ex 0....NULL";
        s = ps.substitute(expr);
        assertEquals(exp, s);

        ps = new PropertiesSubstitutor(Collections.singletonMap("a", "vA"));
        ps.setNullString("NULL");
        expr = "__$$$ __ vA_a_$$a $b/$c$";
        exp = "__$$$ __ vA_a_$vA NULL/NULL$";
        s = ps.substitute(expr);
        assertEquals(exp, s);
    }
}
