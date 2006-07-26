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
package scriptella.expression;

import scriptella.AbstractTestCase;

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
        String s = ps.substitute(exp, new ParametersCallback() {
            public Object getParameter(final String name) {
                fail("No parameters should be asked for. Param name=" + name);
                return null;
            }

            ;
        });
        assertEquals(exp, s);
        exp = "No Params to substitute";
        s = ps.substitute(exp, new ParametersCallback() {
            public Object getParameter(final String name) {
                fail("No parameters should be asked for. Param name=" + name);
                return null;
            }

            ;
        });
        assertEquals(exp, s);


    }

    public void test() {
        PropertiesSubstitutor ps = new PropertiesSubstitutor();

        ParametersCallback c = new ParametersCallback() {
            public Object getParameter(final String name) {
                return "property_" + name;
            }
        };
        String s = ps.substitute("$$ Text${subst1}${subst2}$subst3$subst4 End of test", c);
        assertEquals("$$ Textproperty_subst1property_subst2property_subst3property_subst4 End of test", s);
    }

    public void testNullProperties() {
        PropertiesSubstitutor ps = new PropertiesSubstitutor();

        ParametersCallback c = new ParametersCallback() {
            public Object getParameter(final String name) {
                return null;
            }
        };
        String exp = "$$ Text${subst1}${subst2}$subst3$subst4 End of test";
        String s = ps.substitute(exp, c);
        assertEquals(exp, s);
    }
}
