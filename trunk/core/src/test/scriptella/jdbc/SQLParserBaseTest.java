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
package scriptella.jdbc;

import scriptella.AbstractTestCase;

import java.io.StringReader;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tests {@link scriptella.jdbc.SQLParserBase}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SQLParserBaseTest extends AbstractTestCase {
    public void test() {
        String s = "-not skipped\n--HintOrComment\n" +
                "     \n" +
                "     CREATE TABLE Test (\n" +
                "            ID INT,\n" +
                "            $VALUE VARCHAR(255)\n" +
                "        );\n" +
                " ${extra}\n" +
                "        insert into test(id, value) values (?1,  '?justatext');\n" +
                "        insert into test(id, value) values (?value,'A test?{justatext}');\n" +
                "        insert into test(id, value) values (3,?text);\n" +
                " //comment$justatext ?{justatext} ?justatext\n";
        //comments are ignored and quoted values are not parsed
        final String[] expected = {
                "-not skipped\n--HintOrComment\nCREATE TABLE Test (\n" +
                        "            ID INT,\n" +
                        "            $/VALUE/ VARCHAR(255)\n" +
                        "        )",
                " $/extra/" +
                        "        insert into test(id, value) values (?/1/,  '?justatext')",
                "        insert into test(id, value) values (?/value/,'A test?{justatext}')",
                "        insert into test(id, value) values (3,?/text/)",
                " //comment$justatext ?{justatext} ?justatext\n"};


        final Set<String> exprSet = new TreeSet<String>();
        exprSet.add("extra");
        final Set<String> propSet = new TreeSet<String>();
        propSet.add("1");
        propSet.add("value");
        propSet.add("VALUE");
        propSet.add("text");


        SQLParserBase p = new SQLParserBase() {
            int stInd;

            @Override
            protected String handleParameter(final String name, final boolean expression, boolean jdbcParam) {
                if (expression) {
                    assertTrue(exprSet.contains(name));
                } else {
                    assertTrue("Unexpected "+name+" property", propSet.contains(name));
                }
                return (jdbcParam ? "?/" : "$/") + name + '/';
            }

            protected void statementParsed(final String sql) {
                assertEquals(removeWhitespaceChars(expected[stInd]), removeWhitespaceChars(sql));
                stInd++;
            }
        };
        p.parse(new StringReader(s));

    }



}
