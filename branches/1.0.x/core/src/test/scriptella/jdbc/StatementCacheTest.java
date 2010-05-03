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
package scriptella.jdbc;

import scriptella.DBTestCase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link StatementCache}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class StatementCacheTest extends DBTestCase {


    private int simpleClosed;
    private int preparedClosed;
    private int preparedCleared;
    private int preparedParamsSet;

    StatementCache sc;

    protected void setUp() {
        sc = new StatementCache(null, 100) {
            @Override
            protected StatementWrapper.Simple create(final String sql, final JdbcTypesConverter converter) {
                return new StatementWrapper.Simple(sql) {
                    public void close() {
                        simpleClosed++;
                    }
                };
            }

            @Override
            protected StatementWrapper.Prepared prepare(final String sql, final JdbcTypesConverter converter) {
                return new StatementWrapper.Prepared() {
                    @Override
                    public void close() {
                        preparedClosed++;
                    }

                    @Override
                    public void setParameters(List<Object> params) {
                        preparedParamsSet++;
                    }

                    @Override
                    public void clear() {
                        preparedCleared++;
                    }
                };
            }
        };

    }

    /**
     * Test when number of statement exceeds the cache size.
     *
     * @throws SQLException
     */
    public void testGrowth() throws SQLException {
        simpleClosed = 0;
        preparedClosed = 0;
        preparedCleared = 0;
        preparedParamsSet = 0;
        StringBuilder sb = new StringBuilder();
        JdbcTypesConverter converter = new JdbcTypesConverter();
        List<Object> params = new ArrayList<Object>();
        params.add(1);
        for (int i = 0; i < 105; i++) {
            sb.append('.');
            StatementWrapper s = sc.prepare(sb.toString(), Collections.emptyList(), converter);
            assertEquals(i, preparedParamsSet);//Should be recognized as simple statement
            sc.releaseStatement(s);
            assertEquals(i, simpleClosed);
            StatementWrapper s2 = sc.prepare(sb.toString(), params, converter);
            assertEquals(i + 1, preparedParamsSet); //Should be recognized as a prepared statement
            assertEquals(i + 1, simpleClosed); //statement should be closed
            assertEquals(i, preparedCleared);
            sc.releaseStatement(s2);
            assertEquals(i + 1, preparedCleared);
        }
        //5 statements have to be removed from cache
        assertEquals(5, preparedClosed);
        sc.close();

    }

    public void testUsual() throws SQLException {
        preparedClosed = 0;
        preparedCleared = 0;


        StringBuilder sb = new StringBuilder();
        JdbcTypesConverter converter = new JdbcTypesConverter();
        List<Object> params = new ArrayList<Object>();
        params.add(1);
        for (int i = 0; i < 20; i++) {
            sb.append('.');
            StatementWrapper s2 = sc.prepare(sb.toString(), params, converter);
            sc.releaseStatement(s2);
        }
        assertEquals(20, preparedCleared);
        assertEquals(0, preparedClosed); //no closed stmt if cache is working
        sc.close();
        assertEquals(20, preparedCleared);
        assertEquals(20, preparedClosed);

    }


}
