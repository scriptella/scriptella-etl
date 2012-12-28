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

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

/**
 * Tests for {@link SqlReaderTokenizer}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SqlTokenizerTest extends AbstractTestCase {
    public void test() throws IOException {
        String s="insert into table values 1,?v,\"?text\";st2";
        SqlTokenizer tok = new SqlReaderTokenizer(new StringReader(s));
        String actual = tok.nextStatement();
        assertEquals("insert into table values 1,?v,\"?text\"", actual);
        assertTrue(Arrays.equals(new int[] {27}, tok.getInjections()));
        actual = tok.nextStatement();
        assertEquals("st2", actual);
        assertTrue(tok.getInjections().length==0);

        s="DROP TABLE Test;";
        tok = new SqlReaderTokenizer(new StringReader(s));
        actual = tok.nextStatement();
        assertEquals("DROP TABLE Test", actual);
        actual = tok.nextStatement();
        assertNull(actual);

        s="UPDATE test set value='Updated1' where ID=1;";
        tok = new SqlReaderTokenizer(new StringReader(s));
        actual = tok.nextStatement();
        assertEquals("UPDATE test set value='Updated1' where ID=1", actual);
        actual = tok.nextStatement();
        assertNull(actual);
    }

    public void testComments() throws IOException {
        String s="insert into table values 1,?v--$comment\n;" +
                "-notacomment$v/**fdfdfd$comment\n$comment.v$$???\n;;;*/;stmt${var};\n" +
                "//$comment";
        SqlReaderTokenizer tok = new SqlReaderTokenizer(new StringReader(s));
        tok.setKeepFormat(true);
        assertEquals("insert into table values 1,?v--$comment\n", tok.nextStatement());
        assertTrue(Arrays.equals(new int[] {27}, tok.getInjections()));
        assertEquals("-notacomment$v/**fdfdfd$comment\n$comment.v$$???\n;;;*/", tok.nextStatement());
        assertTrue(Arrays.equals(new int[] {12}, tok.getInjections()));
        assertEquals("stmt${var}", tok.nextStatement());
        assertTrue(Arrays.equals(new int[] {4}, tok.getInjections()));
        tok.nextStatement();
        assertTrue(tok.getInjections().length==0);
    }

    public void testQuotes() throws IOException {
        String data = "INSERT INTO \"$TBL\" VALUES (\"?V\")";
        SqlTokenizer tok = new SqlReaderTokenizer(new StringReader(data));
        tok.nextStatement();
        assertTrue(Arrays.equals(new int[] {13}, tok.getInjections()));
    }

    /**
     * Test correct handling of empty files.
     */
    public void testEmpty() throws IOException {
        SqlTokenizer tok = new SqlReaderTokenizer(new StringReader(""));
        assertNull(tok.nextStatement());
    }

    public void testSeparator() throws IOException {
        String data = "st;1;;st 2";
        SqlReaderTokenizer tok = new SqlReaderTokenizer(new StringReader(data));
        tok.setSeparator(";;");
        assertEquals("st;1", tok.nextStatement());
        assertEquals("st 2", tok.nextStatement());
        assertNull(tok.nextStatement());
        tok = new SqlReaderTokenizer(new StringReader(data));
        tok.setSeparator(";;");
        tok.setSeparatorOnSingleLine(true);
        assertEquals("st;1;;st 2", tok.nextStatement());
        assertNull(tok.nextStatement());
        data = "st;1 \n;;\nst 2";
        tok = new SqlReaderTokenizer(new StringReader(data));
        tok.setSeparator(";;");
        tok.setSeparatorOnSingleLine(true);
        assertEquals("st;1 \n", tok.nextStatement());
        assertEquals("st 2", tok.nextStatement());
        assertNull(tok.nextStatement());
        data = "st;$v1\n / /*?comment*/ ?v2 2";
        tok = new SqlReaderTokenizer(new StringReader(data));
        tok.setSeparator("/");
        tok.setSeparatorOnSingleLine(true);
        tok.setKeepFormat(true);
        assertEquals(data, tok.nextStatement());
        assertTrue(Arrays.equals(new int[] {3, 23}, tok.getInjections()));
        assertNull(tok.nextStatement());
        ///
        data = "st;$v1\r/\n/*?comment*/ ?v2 2";
        tok = new SqlReaderTokenizer(new StringReader(data));
        tok.setSeparator("/");
        tok.setSeparatorOnSingleLine(true);
        tok.setKeepFormat(true);
        assertEquals("st;$v1\r", tok.nextStatement());
        assertTrue(Arrays.equals(new int[] {3}, tok.getInjections()));
        assertEquals("/*?comment*/ ?v2 2", tok.nextStatement());
        assertTrue(Arrays.equals(new int[] {13}, tok.getInjections()));
        assertNull(tok.nextStatement());
        ///
        data = "STATEMENT1 / \n\r" +
                "   / \r\nSTATEMENT2\n/**fdfdfd**//";
        tok = new SqlReaderTokenizer(new StringReader(data));
        tok.setSeparator("/");
        tok.setSeparatorOnSingleLine(true);
        assertEquals("STATEMENT1 / \n", tok.nextStatement());
        assertEquals("\nSTATEMENT2\n" +
                "/", tok.nextStatement());
        ///
        data = "/\nscript\n/\n";
        tok = new SqlReaderTokenizer(new StringReader(data));
        tok.setSeparator("/");
        tok.setSeparatorOnSingleLine(true);
        assertEquals("", tok.nextStatement());
        assertEquals("script\n", tok.nextStatement());
        assertNull(tok.nextStatement());
        ///
        data = "/\nscript\n/";
        tok = new SqlReaderTokenizer(new StringReader(data));
        tok.setSeparator("/");
        tok.setSeparatorOnSingleLine(true);
        assertEquals("", tok.nextStatement());
        assertEquals("script\n", tok.nextStatement());
        assertNull(tok.nextStatement());
        ///
        data = "statement;--comment\n/\nscrip$t\n/";
        tok = new SqlReaderTokenizer(new StringReader(data));
        tok.setSeparator("/");
        tok.setSeparatorOnSingleLine(true);
        assertEquals("statement;\n", tok.nextStatement());
        assertEquals("scrip$t\n", tok.nextStatement());
        assertTrue(Arrays.equals(new int[] {5}, tok.getInjections()));
        assertNull(tok.nextStatement());
    }

    /**
     * Tests if oracle hints are preserved.
     */
    public void testOracleHint() throws IOException {
        String original = "SQL /*+ HINT */ --NOTAHINT \n\r /* +NOTAHINT*/";
        SqlReaderTokenizer tok = new SqlReaderTokenizer(new StringReader(original));
        assertEquals("SQL /*+ HINT */ \n",tok.nextStatement());
        assertNull(tok.nextStatement());
        tok = new SqlReaderTokenizer(new StringReader(original));
        tok.setKeepFormat(true);
        assertEquals(original,tok.nextStatement());
        assertNull(tok.nextStatement());
        //Now test with / separator - result should be the same
        tok = new SqlReaderTokenizer(new StringReader(original));
        tok.setKeepFormat(true);
        tok.setSeparator("/");
        tok.setSeparatorOnSingleLine(true);
        assertEquals(original,tok.nextStatement());
        assertNull(tok.nextStatement());
        //Now test the ?,$ handling
        original = "SQL $v1 ?v2 /*+ HINT */ --?NOT$AHINT \n\r'$v3'/* +$NOTAHINT*/ ?v4";
        tok = new SqlReaderTokenizer(new StringReader(original));
        assertEquals("SQL $v1 ?v2 /*+ HINT */ \n'$v3' ?v4",tok.nextStatement());
        assertTrue(Arrays.equals(new int[] {4,8,26,31},tok.getInjections()));
        //The same check but with keep format
        tok = new SqlReaderTokenizer(new StringReader(original));
        tok.setKeepFormat(true);
        assertEquals(original,tok.nextStatement());
        assertTrue(Arrays.equals(new int[] {4,8,40,60},tok.getInjections()));
        assertNull(tok.nextStatement());
    }

    /**
     * Tests if extra whitespaces are removed in keepformat=false mode.
     * Single whitespace trimming is not performed, because performance is more important. 
     */
    public void testWhitespaceTrim() throws IOException {
        String sql = "    --Comment\n\n\n               SQL--text\n;   SQL2";
        SqlTokenizer tok = new SqlReaderTokenizer(new StringReader(sql));
        assertEquals(" \nSQL\n", tok.nextStatement());
        assertEquals(" SQL2", tok.nextStatement());
        assertNull(tok.nextStatement());
    }


}
