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

import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

/**
 * Performance tests for {@link scriptella.expression.LineIterator}.
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class LineIteratorPerfTest extends AbstractTestCase {
   /**
     * History:
     * 18.03.2008 - Duron 1.7Mhz - 750 ms
     */
    public void test() {
        String line = "demo text line;demo text line;demo text line;demo text line;demo text line;demo text line;demo text line;\n" +
                      "demo text line2;demo text line2;demo text line2;demo text line2;demo text line2;demo text line2;\n";
        final int n = 10;
        StringBuilder res = new StringBuilder(line.length()*n);


        for (int i=0;i< n;i++) {
            res.append(line);
        }
        final String s = res.toString();
        final Map<String, String> params = Collections.emptyMap();
        for (int i=0;i<10000;i++) {
            LineIterator it = new LineIterator(new StringReader(s));
            while (it.hasNext()) {
                it.next();
            }
            it = new LineIterator(new StringReader(s), new PropertiesSubstitutor(params),true);
            while (it.hasNext()) {
                it.next();
            }
        }
    }
}
