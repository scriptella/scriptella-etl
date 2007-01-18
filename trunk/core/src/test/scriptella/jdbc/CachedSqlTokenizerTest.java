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

/**
 * Tests for {@link scriptella.jdbc.CachedSqlTokenizer}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class CachedSqlTokenizerTest extends AbstractTestCase {
    public void test() throws IOException {
        SqlTokenizer target = new SqlReaderTokenizer(new StringReader("s1;s2;s3?v"));
        SqlTokenizer tok = new CachedSqlTokenizer(target);
        String[] expectedSt = new String[] {"s1", "s2", "s3?v"};
        int[][] expectedInj = new int[][] {{}, {}, {2}};
        for (int i=0;i<10000;i++) {
            for (int j=0;j<3;j++) {
                String s = tok.nextStatement();
                assertEquals(expectedSt[j], s);
                assertTrue(Arrays.equals(expectedInj[j], tok.getInjections()));
            }
            assertNull(tok.nextStatement());
            assertTrue(tok.getInjections().length==0); //Null would be better, but need to fix SqlReaderTokenizer
            tok.close();
        }
    }

    /**
     * Tests if closing cached tokenizer allows to refetch
     * @throws IOException
     */
    public void testClose() throws IOException {
        SqlTokenizer target = new SqlReaderTokenizer(new StringReader("s1;s2;s3?v"));
        SqlTokenizer tok = new CachedSqlTokenizer(target);
        tok.nextStatement();
        tok.close();
        assertEquals("s1", tok.nextStatement());
        assertEquals("s2", tok.nextStatement());
    }
}
