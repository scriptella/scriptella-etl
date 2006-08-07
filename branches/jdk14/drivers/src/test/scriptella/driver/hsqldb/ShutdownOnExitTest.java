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
package scriptella.driver.hsqldb;

import scriptella.AbstractTestCase;
import scriptella.jdbc.JDBCConnection;
import scriptella.jdbc.JDBCUtils;
import scriptella.spi.ConnectionParameters;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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

        final String url = "jdbc:hsqldb:mem:shutdowntest";
        ConnectionParameters params = new ConnectionParameters(props, url, "sa", null, null, null, null);
        JDBCConnection con = drv.connect(params);
        Connection nc = con.getNativeConnection();
        Statement st = nc.createStatement();

        st.execute("        CREATE TABLE Test (ID INT);");
        props.put("ifexists", "true"); //do not create new database if not exists
        ConnectionParameters params2 = new ConnectionParameters(props, url, "sa", null, null, null, null);

        JDBCConnection con2 = drv.connect(params2);
        con2.close();
        con.close();
        Driver.HOOK.run(); //emulates shutdown hook
        assertDatabaseShutdown(url);
    }

    private void assertDatabaseShutdown(String url) {
        Properties props = new Properties();
        props.put("ifexists", "true"); //do not create new database if not exists
        props.put("user", "sa");
        try {
            final Connection con = DriverManager.getConnection(url, props);
            JDBCUtils.closeSilent(con);
        } catch (SQLException e) {
            assertTrue("Driver.HOOK must shutdown the database " + url, e.getErrorCode() == -94);
            return; //OK
        } catch (Exception e) {
            e.printStackTrace();
        }
        fail("Driver.HOOK must shutdown the database");
    }


    /**
     * Tests if no failures occurs on attempt to close connection for shutdown database
     *
     * @throws SQLException
     */
    public void testAlreadyClosed() throws SQLException {
        Driver drv = new Driver();
        Map<String, String> props = new HashMap<String, String>();

        final String url = "jdbc:hsqldb:mem:alreadyClosed";
        ConnectionParameters params = new ConnectionParameters(props, url, "sa", null, null, null, null);
        JDBCConnection con = drv.connect(params);
        Connection nc = con.getNativeConnection();
        nc.createStatement().execute("SHUTDOWN;");
        con.close();
        Driver.HOOK.run(); //emulates shutdown hook
    }

    /**
     * Tests if different databases are independently shutdown, i.e. closing one db
     * does not affect other.
     *
     * @throws SQLException
     */
    public void testDifferentDbs() throws SQLException {
        Driver drv = new Driver();
        Map<String, String> props = new HashMap<String, String>();

        //Create first db and obtain 2 connections
        String url1 = "jdbc:hsqldb:mem:DifferentDbs1";
        ConnectionParameters params = new ConnectionParameters(props, url1, "sa", null, null, null, null);
        JDBCConnection con1 = drv.connect(params);
        JDBCConnection con11 = drv.connect(params);
        //Create second db and obtain 2 connections
        String url2 = "jdbc:hsqldb:mem:DifferentDbs2";
        params = new ConnectionParameters(props, url2, "sa", null, null, null, null);
        JDBCConnection con2 = drv.connect(params);
        JDBCConnection con22 = drv.connect(params);
        //close everything
        con1.close();
        con2.close();
        con11.close();
        con22.close();
        Driver.HOOK.run(); //emulates shutdown hook
        //all databases should be correctly shutdown
        assertDatabaseShutdown(url1);
        assertDatabaseShutdown(url2);
    }


}


