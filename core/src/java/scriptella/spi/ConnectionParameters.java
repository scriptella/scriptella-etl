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

package scriptella.spi;

import java.util.Map;

/**
 * Represents connection parameters.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConnectionParameters {

    private Map<String, String> properties;
    private String url;
    private String user;
    private String password;
    private String schema;
    private String catalog;

    /**
     * Creates connection parameters based on &lt;connection&gt; element..
     */
    public ConnectionParameters(Map<String, String> properties, String url, String user, String password, String schema, String catalog) {
        this.properties = properties;
        this.url = url;
        this.user = user;
        this.password = password;
        this.schema = schema;
        this.catalog = catalog;
    }

    /**
     * This method returns properties for connection specified inside &lt;connection&gt; element
     *
     * @return properties map.
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Convenience method which returns property by name.
     *
     * @param name property name
     * @return property value
     * @see #getProperties()
     */
    public String getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Returns connection URL.
     * <p>URL format is arbitrary and specified by Service Provider.
     *
     * @return connection URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Optional user name for connection.
     *
     * @return user name
     */
    public String getUser() {
        return user;
    }

    /**
     * Optional user password for connection.
     *
     * @return user password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return optional schema name
     */
    public String getSchema() {
        return schema;
    }

    /**
     * @return optional catalog name
     */
    public String getCatalog() {
        return catalog;
    }


    public String toString() {
        return "ConnectionParameters{" +
                "properties=" + properties +
                ", url='" + url + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", schema='" + schema + '\'' +
                ", catalog='" + catalog + '\'' +
                '}';
    }
}
