/*
 * Copyright 2006-2007 The Scriptella Project Team.
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
        tok.setKeepFormat(true);
        assertEquals("insert into table values 1,?v--$comment\n", tok.nextStatement().toString());
        assertEquals(Arrays.asList(new Integer[] {27}), tok.getInjections());
        assertEquals("-notacomment$v/**fdfdfd$comment\n$comment.v$$???\n;;;*/", tok.nextStatement().toString());
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

    public void testSeparator() throws IOException {
        String data = "st;1;;st 2";
        SqlTokenizer tok = new SqlTokenizer(new StringReader(data));
        tok.setSeparator(";;");
        assertEquals("st;1", tok.nextStatement().toString());
        assertEquals("st 2", tok.nextStatement().toString());
        assertNull(tok.nextStatement());
        tok = new SqlTokenizer(new StringReader(data));
        tok.setSeparator(";;");
        tok.setSeparatorOnSingleLine(true);
        assertEquals("st;1;;st 2", tok.nextStatement().toString());
        assertNull(tok.nextStatement());
        data = "st;1 \n;;\nst 2";
        tok = new SqlTokenizer(new StringReader(data));
        tok.setSeparator(";;");
        tok.setSeparatorOnSingleLine(true);
        assertEquals("st;1 \n", tok.nextStatement().toString());
        assertEquals("st 2", tok.nextStatement().toString());
        assertNull(tok.nextStatement());
        data = "st;$v1\n / /*?comment*/ ?v2 2";
        tok = new SqlTokenizer(new StringReader(data));
        tok.setSeparator("/");
        tok.setSeparatorOnSingleLine(true);
        tok.setKeepFormat(true);
        assertEquals(data, tok.nextStatement().toString());
        assertEquals(Arrays.asList(new Integer[] {3, 23}), tok.getInjections());
        assertNull(tok.nextStatement());
        ///
        data = "st;$v1\r/\n/*?comment*/ ?v2 2";
        tok = new SqlTokenizer(new StringReader(data));
        tok.setSeparator("/");
        tok.setSeparatorOnSingleLine(true);
        tok.setKeepFormat(true);
        assertEquals("st;$v1\r", tok.nextStatement().toString());
        assertEquals(Arrays.asList(new Integer[] {3}), tok.getInjections());
        assertEquals("/*?comment*/ ?v2 2", tok.nextStatement().toString());
        assertEquals(Arrays.asList(new Integer[] {13}), tok.getInjections());
        assertNull(tok.nextStatement());
        ///
        data = "STATEMENT1 / \n\r" +
                "   / \r\nSTATEMENT2\n/**fdfdfd**//";
        tok = new SqlTokenizer(new StringReader(data));
        tok.setSeparator("/");
        tok.setSeparatorOnSingleLine(true);
        assertEquals("STATEMENT1 / \n", tok.nextStatement().toString());
        assertEquals("\nSTATEMENT2\n" +
                "/", tok.nextStatement().toString());
        ///
        data = "/\nscript\n/\n";
        tok = new SqlTokenizer(new StringReader(data));
        tok.setSeparator("/");
        tok.setSeparatorOnSingleLine(true);
        assertEquals("", tok.nextStatement().toString());
        assertEquals("script\n", tok.nextStatement().toString());
        assertNull(tok.nextStatement());
        ///
        data = "/\nscript\n/";
        tok = new SqlTokenizer(new StringReader(data));
        tok.setSeparator("/");
        tok.setSeparatorOnSingleLine(true);
        assertEquals("", tok.nextStatement().toString());
        assertEquals("script\n", tok.nextStatement().toString());
        assertNull(tok.nextStatement());
        ///
        data = "statement;--comment\n/\nscrip$t\n/";
        tok = new SqlTokenizer(new StringReader(data));
        tok.setSeparator("/");
        tok.setSeparatorOnSingleLine(true);
        assertEquals("statement;\n", tok.nextStatement().toString());
        assertEquals("scrip$t\n", tok.nextStatement().toString());
        assertEquals(Arrays.asList(new Integer[] {5}), tok.getInjections());
        assertNull(tok.nextStatement());
    }

    /**
     * Tests if oracle hints are preserved.
     */
    public void testOracleHint() throws IOException {
        String original = "SQL /*+ HINT */ --NOTAHINT \n\r /* +NOTAHINT*/";
        SqlTokenizer tok = new SqlTokenizer(new StringReader(original));
        assertEquals("SQL /*+ HINT */ \n",tok.nextStatement().toString());
        assertNull(tok.nextStatement());
        tok = new SqlTokenizer(new StringReader(original));
        tok.setKeepFormat(true);
        assertEquals(original,tok.nextStatement().toString());
        assertNull(tok.nextStatement());
        //Now test with / separator - result should be the same
        tok = new SqlTokenizer(new StringReader(original));
        tok.setKeepFormat(true);
        tok.setSeparator("/");
        tok.setSeparatorOnSingleLine(true);
        assertEquals(original,tok.nextStatement().toString());
        assertNull(tok.nextStatement());
        //Now test the ?,$ handling
        original = "SQL $v1 ?v2 /*+ HINT */ --?NOT$AHINT \n\r'$v3'/* +$NOTAHINT*/ ?v4";
        tok = new SqlTokenizer(new StringReader(original));
        assertEquals("SQL $v1 ?v2 /*+ HINT */ \n'$v3' ?v4",tok.nextStatement().toString());
        assertEquals(Arrays.asList(new Integer[] {4,8,26,31}),tok.getInjections());
        //The same check but with keep format
        tok = new SqlTokenizer(new StringReader(original));
        tok.setKeepFormat(true);
        assertEquals(original,tok.nextStatement().toString());
        assertEquals(Arrays.asList(new Integer[] {4,8,40,60}),tok.getInjections());
        assertNull(tok.nextStatement());
    }

    /**
     * Tests if extra whitespaces are removed in keepformat=false mode.
     * Single whitespace trimming is not performed, because performance is more important. 
     */
    public void testWhitespaceTrim() throws IOException {
        String sql = "    --Comment\n\n\n               SQL--text\n;   SQL2";
        SqlTokenizer tok = new SqlTokenizer(new StringReader(sql));
        assertEquals(" \nSQL\n", tok.nextStatement().toString());
        assertEquals(" SQL2", tok.nextStatement().toString());
        assertNull(tok.nextStatement());
    }


}
