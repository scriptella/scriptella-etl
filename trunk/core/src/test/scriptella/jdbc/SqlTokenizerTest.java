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

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;

/**
 * Tests for {@link SqlTokenizer}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SqlTokenizerTest extends AbstractTestCase {
    public void test() throws IOException {
        String s="insert into table values 1,?v,\"?text\";st2";
        SqlTokenizer tok = new SqlTokenizer(new StringReader(s));
        StringBuilder actual = tok.nextStatement();
        assertEquals("insert into table values 1,?v,\"?text\"", actual.toString());
        assertEquals(Arrays.asList(new Integer[] {27}), tok.getInjections());
        actual = tok.nextStatement();
        assertEquals("st2", actual.toString());
        assertTrue(tok.getInjections().isEmpty());

        s="DROP TABLE Test;";
        tok = new SqlTokenizer(new StringReader(s));
        actual = tok.nextStatement();
        assertEquals("DROP TABLE Test", actual.toString());
        actual = tok.nextStatement();
        assertNull(actual);

        s="UPDATE test set value='Updated1' where ID=1;";
        tok = new SqlTokenizer(new StringReader(s));
        actual = tok.nextStatement();
        assertEquals("UPDATE test set value='Updated1' where ID=1", actual.toString());
        actual = tok.nextStatement();
        assertNull(actual);
    }

    public void testComments() throws IOException {
        String s="insert into table values 1,?v--$comment\n;" +
                "-notacomment$v/**fdfdfd$comment\n$comment.v$$???\n;;;*/;stmt${var};\n" +
                "//$comment";
        SqlTokenizer tok = new SqlTokenizer(new StringReader(s));
        tok.nextStatement();
        assertEquals(Arrays.asList(new Integer[] {27}), tok.getInjections());
        tok.nextStatement();
        assertEquals(Arrays.asList(new Integer[] {12}), tok.getInjections());
        assertEquals("stmt${var}", tok.nextStatement().toString());
        assertEquals(Arrays.asList(new Integer[] {4}), tok.getInjections());
        tok.nextStatement();
        assertEquals(Collections.EMPTY_LIST, tok.getInjections());
    }

    public void testQuotes() throws IOException {
        String data = "INSERT INTO \"$TBL\" VALUES (\"?V\")";
        SqlTokenizer tok = new SqlTokenizer(new StringReader(data));
        tok.nextStatement();
        assertEquals(Arrays.asList(new Integer[] {13}), tok.getInjections());
    }

    /**
     * Test correct handling of empty files.
     */
    public void testEmpty() throws IOException {
        SqlTokenizer tok = new SqlTokenizer(new StringReader(""));
        assertNull(tok.nextStatement());
    }

}
