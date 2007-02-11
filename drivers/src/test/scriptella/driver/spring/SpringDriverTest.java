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
package scriptella.driver.spring;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import scriptella.AbstractTestCase;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Tests for {@link Driver}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SpringDriverTest extends AbstractTestCase {
    public void test() throws SQLException, ClassNotFoundException, EtlExecutorException {
        BeanFactory bf = new ClassPathXmlApplicationContext("scriptella/driver/spring/springbeans.xml");
        DataSource ds = (DataSource) bf.getBean("datasource"); //Test if bean factory contain correct data
        Connection con = ds.getConnection();
        con.createStatement().executeQuery("select * from AutoStart"); //A table should be created on startup
        EtlExecutor exec = (EtlExecutor) bf.getBean("executor");
        exec.execute();
        con.createStatement().executeQuery("select * from SpringTable"); //A table should be created
        con.createStatement().execute("SHUTDOWN");

    }
}
