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
package scriptella.driver.h2;

import scriptella.AbstractTestCase;
import scriptella.execution.ScriptsExecutor;
import scriptella.execution.ScriptsExecutorException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests a script working with H2.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class H2ScriptTest extends AbstractTestCase {
    /**
     * Runs the script and checks if transformations has been made.
     *
     * @throws ScriptsExecutorException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public void test() throws ScriptsExecutorException, SQLException, ClassNotFoundException, IOException {
        Class.forName("org.h2.Driver");
        //Opening a connection before executing a script to disable shutdown on last connection close.
        Connection con = DriverManager.getConnection("jdbc:h2:mem:tst");
        ScriptsExecutor se = newScriptsExecutor();
        se.execute();
        ResultSet rs = con.createStatement().executeQuery("SELECT * FROM Test ORDER BY ID");
        List actual = new ArrayList();
        while (rs.next()) {
            Integer n = (Integer) rs.getObject(1);
            actual.add(n);
            byte[] expBlob = new byte[4];
            Arrays.fill(expBlob, n.byteValue());
            ByteArrayInputStream bais = (ByteArrayInputStream) rs.getObject(2);
            byte[] actualBlob = new byte[4];
            bais.read(actualBlob);
            assertTrue(bais.read()<0); //no bytes are left

            assertTrue(Arrays.equals(expBlob, actualBlob));

        }
        List exp = Arrays.asList(1, 2, 3);
        assertEquals(exp, actual);


    }
}
