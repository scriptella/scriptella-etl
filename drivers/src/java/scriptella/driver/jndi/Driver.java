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
package scriptella.driver.jndi;

import scriptella.jdbc.GenericDriver;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Scriptella driver for JNDI datasources.
 * <p>This driver relies on {@link scriptella.jdbc.GenericDriver} functionality.
 * <p><em>Note:</em>Currently this driver does not support JTA transactions
 * and simply use connections provided by datasource.
 * In this case new-tx has no effect if code is runned inside a transaction.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class Driver extends GenericDriver {
    @Override
    protected java.sql.Connection getConnection(String url, Properties props) throws SQLException {
        if (url == null) {
            throw new JndiProviderException("JNDI name must be specified in an url attribute of connection element.");
        }
        try {
            InitialContext ctx = new InitialContext(props);
            if (url.startsWith("jndi:")) { //Remove jndi: URL prefix used for autodiscovery
                url = url.substring(5);
            }
            DataSource ds = (DataSource) ctx.lookup(url);
            return ds.getConnection();
        } catch (NamingException e) {
            throw new JndiProviderException("A problem occured while trying to lookup a datasource with name " + url, e);
        }

    }
}
