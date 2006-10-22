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

import java.nio.charset.Charset;
import java.text.ParseException;
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
    private DriverContext context;

    /**
     * Creates connection parameters based on &lt;connection&gt; element..
     */
    public ConnectionParameters(Map<String, String> properties, String url, String user, String password,
                                String schema, String catalog, DriverContext context) {
        this.properties = properties;
        this.url = url;
        this.user = user;
        this.password = password;
        this.schema = schema;
        this.catalog = catalog;
        this.context = context;
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
     * @see #getBooleanProperty(String, boolean)
     */
    public boolean getBooleanProperty(String name) throws ParseException {
        return getBooleanProperty(name, false);
    }

    /**
     * Parses property value as boolean flag.
     * @param name property name.
     * @param defaultValue default value to use if connection has no such property.
     * @return boolean property value.
     * @throws ParseException if property has unrecognized value.
     */
    public boolean getBooleanProperty(String name, boolean defaultValue) throws ParseException {
        String a = getProperty(name);
        if (a==null) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(a) || "1".equalsIgnoreCase(a) ||
                "on".equalsIgnoreCase(a) || "yes".equalsIgnoreCase(a)) {
            return true;
        }

        if ("false".equalsIgnoreCase(a) || "0".equalsIgnoreCase(a) ||
                "off".equalsIgnoreCase(a) || "no".equalsIgnoreCase(a)) {
            return false;
        }
        throw new ParseException("Unrecognized boolean property value "+a,0);
    }

    /**
     * Parses property value as a charset encoding name.
     * @param name property name.
     * @return value of the property or null if connection has no such property.
     * @throws ParseException if charset name is unsupported.
     */
    public String getCharsetProperty(String name) throws ParseException {
        String enc = getProperty(name);
        if (enc != null && !Charset.isSupported(enc)) {
            throw new ParseException("Specified encoding " +
                    enc+ " is not supported. Supported encodings are " + Charset.availableCharsets().keySet(),0);
        }
        return enc;
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

    /**
     * Get a drivers context.
     * @return script context.
     */
    public DriverContext getContext() {
        return context;
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
