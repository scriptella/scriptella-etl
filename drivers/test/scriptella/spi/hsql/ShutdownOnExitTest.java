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
package scriptella.spi.hsql;

import scriptella.AbstractTestCase;
import scriptella.jdbc.JDBCConnection;
import scriptella.jdbc.JDBCException;
import scriptella.spi.ConnectionParameters;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests shutdown on exit functionality.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ShutdownOnExitTest extends AbstractTestCase {
    public void test() throws SQLException {
        Driver drv = new Driver();
        Map<String, String> props = new HashMap<String, String>();
        props.put(Driver.SHUTDOWN_ON_EXIT, "true");

        ConnectionParameters params = new ConnectionParameters(props, "jdbc:hsqldb:mem:shutdowntest", "sa", null, null, null);
        JDBCConnection con = drv.connect(params);
        Connection nc = con.getNativeConnection();
        Statement st = nc.createStatement();

        st.execute("        CREATE TABLE Test (ID INT);");
        props.put("ifexists", "true"); //do not create new database if not exists
        ConnectionParameters params2 = new ConnectionParameters(props, "jdbc:hsqldb:mem:shutdowntest", "sa", null, null, null);

        JDBCConnection con2 = drv.connect(params2);
        con2.close();
        con.close();
        Driver.HOOK.run(); //emulates shutdown hook
        try {
            drv.connect(params2);
        } catch (JDBCException e) {
            assertTrue("Driver.HOOK must shutdown the database", e.getErrorCodes().contains("-94"));
            return; //OK
        } catch (Exception e) {

        }
        fail("Driver.HOOK must shutdown the database");
    }

}


