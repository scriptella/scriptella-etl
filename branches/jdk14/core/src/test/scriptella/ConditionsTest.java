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
package scriptella;

import scriptella.execution.ScriptsExecutor;
import scriptella.execution.ScriptsExecutorException;
import scriptella.jdbc.JDBCUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConditionsTest extends DBTestCase {
    public void test() throws ScriptsExecutorException, SQLException {
        final Connection con = getConnection("conditionstest");
        final ScriptsExecutor se = newScriptsExecutor();
        se.execute();

        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = con.prepareStatement("select * from Test");
            rs = ps.executeQuery();

            Set<Integer> expected = new HashSet<Integer>();
            expected.add(1);
            expected.add(3);
            expected.add(4);

            List<Integer> actual = new ArrayList<Integer>();

            //resultset must contains only 1,3 and 4
            while (rs.next()) {
                actual.add(rs.getInt(1));
            }

            assertEquals("Set must be " + expected, 3, actual.size());
            assertTrue("Set must be " + expected, expected.containsAll(actual));
        } finally {
            JDBCUtils.closeSilent(rs);
            JDBCUtils.closeSilent(ps);
        }
    }
}
