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
package scriptella.driver.text;

import scriptella.AbstractTestCase;
import scriptella.spi.AbstractConnection;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link scriptella.driver.text.TextQueryExecutor}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class TextScriptExecutorTest extends AbstractTestCase {
    /**
     * Tests general functionality.
     */
    public void test() throws IOException {
        String s = "  $rownum;$name;$surname;${email.trim().replaceAll('@','_at_')}\n";
        StringWriter out = new StringWriter();
        TextScriptExecutor ts = new TextScriptExecutor(out, true, "\n");
        Map<String,String> m = new HashMap<String, String>();
        m.put("rownum", "1");
        m.put("name", "John");
        m.put("surname", "G");
        m.put("email", "  john@nosuchhost.com");

        ParametersCallback c = MockParametersCallbacks.fromMap(m);
        AbstractConnection.StatementCounter counter = new AbstractConnection.StatementCounter();
        ts.execute(new StringReader(s), c, counter);
        assertEquals(1, counter.statements);

        m.put("rownum", "2");
        m.put("name", "Jim");
        m.put("surname", "G");
        m.put("email", "  jim@nosuchhost.com");
        ts.execute(new StringReader(s), c, counter);
        assertEquals(2, counter.statements);
        ts.close();
        String res = out.toString();
        String[] ar = res.split("\n");
        assertEquals("1;John;G;john_at_nosuchhost.com", ar[0]);
        assertEquals("2;Jim;G;jim_at_nosuchhost.com", ar[1]);
    }

}
