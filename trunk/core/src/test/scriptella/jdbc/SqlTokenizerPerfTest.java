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
package scriptella.jdbc;

import scriptella.AbstractTestCase;
import scriptella.util.RepeatingInputStream;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Performance tests for {@link SqlReaderTokenizer}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SqlTokenizerPerfTest extends AbstractTestCase {
    //600
    public void test() throws IOException {
        String text = "INSERT INTO Table VALUES(?v,$v2);--Hint $v;\n" +
                "/* Comment */\n//comment\nUPDATE Test SET V=?{v2}";
        RepeatingInputStream ris = new RepeatingInputStream(text.getBytes(), 20000);
        SqlTokenizer tok = new SqlReaderTokenizer(new InputStreamReader(ris));
        while (tok.nextStatement()!=null) {
        }
    }
    //700
    public void testNewLineSeparator() throws IOException {
        String text = "INSERT INTO Table VALUES(?v,$v2);--Hint $v\n  / \r" +
                "/* Comment */\n//comment\nUPDATE Test SET V=?{v2}";
        RepeatingInputStream ris = new RepeatingInputStream(text.getBytes(), 20000);
        SqlTokenizer tok = new SqlReaderTokenizer(new InputStreamReader(ris));
        while (tok.nextStatement()!=null) {
        }
    }

}
