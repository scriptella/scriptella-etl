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
package scriptella.driver.alljdbc;

import scriptella.AbstractTestCase;
import scriptella.execution.ScriptsExecutor;
import scriptella.execution.ScriptsExecutorException;
import scriptella.jdbc.JDBCUtils;
import scriptella.spi.DriversFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Test validity data conversion between databases
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class AllJDBCDriversTest extends AbstractTestCase {
    private static List<Object[]> rows = new ArrayList<Object[]>(); //modified by janino
    private static final String[] drivers;
    private static final String[] urls;
    private static final String[] users;
    private static final String[] passwords;
    private List<Connection> connections;

    static {
        //reading test properties containing a set of drivers
        Properties props = new Properties();
        InputStream is = AllJDBCDriversTest.class.getResourceAsStream("test.properties");
        try {
            props.load(is);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        drivers = split(props.getProperty("drivers"));
        urls = split(props.getProperty("urls"));
        users = split(props.getProperty("users"));
        passwords = split(props.getProperty("passwords"));

    }


    /**
     * Callback method to be called by Janino script in xml file.
     * @param row selected row.
     */
    public static void addRow(Object[] row) {
        rows.add(row);
    }

    private static String[] split(String value) {
        String[] strings = (' '+value+' ').split(",");
        for (int i = 0; i < strings.length; i++) {
            strings[i]=strings[i].trim();
        }
        return strings;
    }


    private ScriptsExecutor schema = newScriptsExecutor("schema.xml");

    public void test() throws ScriptsExecutorException, ClassNotFoundException {
        int n = drivers.length;
        //just to make sure properties are valid
        assertTrue(n == urls.length && n == users.length && n == passwords.length);

        ScriptsExecutor se = newScriptsExecutor();

        Map<String,String> props = new HashMap<String, String>();
        //test any combination of drivers in both directions
        for (int i = 0; i < n; i++) {
            props.put("driver1", drivers[i]);
            props.put("url1", urls[i]);
            props.put("user1", users[i]);
            props.put("password1", passwords[i]);
            for (int j = 0; j < n; j++) {
                if (j != i) {
                    props.put("driver2", drivers[j]);
                    props.put("url2", urls[j]);
                    props.put("user2", users[j]);
                    props.put("password2", passwords[j]);
                    se.setExternalProperties(props);
                    se.execute();
                    assertEquals(1, rows.size());
                    Object[] row = rows.get(0);
                    rows.clear();//clear the callback variable for the next loop
                    //checks for columns
                    checkId(row[0]);
                    checkNum(row[1]);
                    checkStr(row[2]);
                    checkFlag(row[3]);
                    checkTi(row[4]);
                    checkData(row[5]);
                    checkBData(row[6]);
                    checkCData(row[7]);
                }
            }
        }
    }

    protected void setUp() throws Exception {
        connections = new ArrayList<Connection>(drivers.length);
        for (int i = 0; i < drivers.length; i++) {
            String driver = drivers[i];
            String url = urls[i];
            String user = users[i];
            String password = passwords[i];
            Map<String,String> props = new HashMap<String, String>();
            props.put("driver",driver);
            props.put("url",url);
            props.put("user",user);
            props.put("password",password);
            try {
                Properties p = new Properties();
                p.load(getClass().getResourceAsStream(driver+".types.properties"));
                props.putAll((Map)p);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            //Initialize a driver and obtain a connection to turn off automatic shutdown on last connection close
            try {
                DriversFactory.getDriver(driver, getClass().getClassLoader());
                connections.add(DriverManager.getConnection(url, user,password));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            schema.setExternalProperties(props);
            schema.execute();
        }
    }

    protected void tearDown() throws Exception {
        for (Connection connection : connections) {
            JDBCUtils.closeSilent(connection);
        }
    }

    private void checkId(Object id) {
        assertEquals(1, id);
    }

    private void checkNum(Object num) {
        assertEquals(3.14, num);
    }

    private void checkStr(Object str) {
        assertEquals("String", str);
    }

    private void checkFlag(Object flag) {
        assertEquals(true, flag);
    }

    private void checkTi(Object ti) {
        assertEquals(Timestamp.valueOf("2006-07-21 19:43:00"), ti);
    }

    public void checkData(Object data) {
        byte[] exp = new byte[]{1, 1, 1, 1};
        assertTrue(Arrays.equals(exp, (byte[]) data));
    }

    public void checkBData(Object bdata) {
        checkData(bdata);
    }

    public void checkCData(Object cdata) {
        String exp = "abcdefg";
        assertEquals(exp, cdata);
    }


}
