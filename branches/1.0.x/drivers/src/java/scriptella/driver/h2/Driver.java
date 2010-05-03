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
package scriptella.driver.h2;

import scriptella.jdbc.GenericDriver;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Scriptella Adapter for H2 database.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class Driver extends GenericDriver {
    public static final String H2_DRIVER_NAME = "org.h2.Driver";


    public Driver() {
        loadDrivers(H2_DRIVER_NAME);
    }

    @Override
    protected Connection getConnection(final String url, final Properties props) throws SQLException {
        String h2Url = url;
        if (h2Url == null || h2Url.length()==0) { //if no url, use the default one
            h2Url = "jdbc:h2:mem:";//private in-memory database connection
        }
        return super.getConnection(h2Url, props);
    }

}
