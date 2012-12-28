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
package scriptella.driver.spring;

import org.springframework.beans.factory.BeanFactory;
import scriptella.jdbc.GenericDriver;
import scriptella.util.StringUtils;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Scriptella driver for <a href="http://www.springframework.org">The Spring Framework</a>
 * Bean Factory(or Application Context) registered datasources.
 * <p>This driver relies on {@link scriptella.jdbc.GenericDriver} functionality.
 * <p><em>Note:</em>Use Spring transaction proxies for
 * {@link EtlExecutorBean} to support Spring transactions.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class Driver extends GenericDriver {

    public Driver() {
        try {
            Class.forName("org.springframework.beans.factory.BeanFactory");
        } catch (ClassNotFoundException e) {
            throw new SpringProviderException("Spring not found on classpath. " +
                    "This driver can be used only in a Spring-managed environment, " +
                    "use EtlExecutorBean to run the ETL file.");
        }
    }

    @Override
    protected java.sql.Connection getConnection(String url, Properties props) throws SQLException {
        if (url == null) {
            throw new SpringProviderException("Name of the spring bean must be specified in an url attribute of connection element.");
        }
        try {
            BeanFactory beanFactory = EtlExecutorBean.getContextBeanFactory();
            DataSource ds = (DataSource) beanFactory.getBean(StringUtils.removePrefix(url, "spring:"));
            return ds.getConnection();
        } catch (Exception e) {
            throw new SpringProviderException("A problem occured while trying to lookup a datasource with name " + url, e);
        }

    }


}
