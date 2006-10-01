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
import scriptella.spi.MockParametersCallbacks;

import java.util.Arrays;

/**
 * Tests {@link scriptella.expression.PropertiesSubstitutor} performance.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class PropertiesSubstitutorPerfTest extends AbstractTestCase {
    /**
     * History:
     * 05.09.2006 - Duron 1700 Mhz - 4078 ms
     */
    public void test() {
        PropertiesSubstitutor ps = new PropertiesSubstitutor(MockParametersCallbacks.NAME);

        char [] fillC = new char[1000];
        Arrays.fill(fillC,'-');
        String fill = new String(fillC);

        String line = "Text ${subst1} $subst2 "+fill+":$end //";
        String exp = "Text subst1 subst2 "+fill+":end //";



        for (int i=0;i<100000;i++) {
            String s = ps.substitute(line);
            assertEquals(exp, s);
        }
    }
}
