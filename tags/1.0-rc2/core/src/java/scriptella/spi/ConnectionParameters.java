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

package scriptella.spi;

import scriptella.configuration.ConfigurationException;
import scriptella.configuration.ConnectionEl;
import scriptella.util.ExceptionUtils;
import scriptella.util.IOUtils;
import scriptella.util.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents connection parameters.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConnectionParameters {
    private Map<String, ?> properties;
    private String url;
    private String user;
    private String password;
    private String schema;
    private String catalog;
    private DriverContext context;

    /**
     * For testing purposes.
     */
    protected ConnectionParameters() {
    }

    /**
     * Creates connection parameters based on &lt;connection&gt; element..
     *
     * @param conf    connection declaration element.
     * @param context driver context.
     */
    public ConnectionParameters(ConnectionEl conf, DriverContext context) {
        this.properties = conf.getProperties();
        this.url = conf.getUrl();
        this.user = conf.getUser();
        this.password = conf.getPassword();
        this.schema = conf.getSchema();
        this.catalog = conf.getCatalog();

        this.context = context;
    }

    /**
     * This method returns properties for connection specified inside &lt;connection&gt; element
     *
     * @return properties map.
     */
    public Map<String, ?> getProperties() {
        return properties;
    }

    /**
     * Convenience method which returns property by name.
     *
     * @param name property name
     * @return property value
     * @see #getProperties()
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Returns string value of the property.
     *
     * @param name property name.
     * @return property value.
     */
    public String getStringProperty(String name) {
        Object v = properties.get(name);
        return v == null ? null : v.toString();
    }

    /**
     * Returns the value of integer property.
     *
     * @param name         name of the property.
     * @param defaultValue default value if property is absent.
     * @return property value.
     * @throws scriptella.configuration.ConfigurationException
     *          if parsing failed.
     * @see #getNumberProperty(String,Number)
     */
    public Integer getIntegerProperty(String name, int defaultValue) throws ConfigurationException {
        return getNumberProperty(name, defaultValue).intValue();
    }

    public Integer getIntegerProperty(String name) throws ConfigurationException {
        Number res = getNumberProperty(name, null);
        return res == null ? null : res.intValue();
    }

    /**
     * Returns numeric value of the property.
     * <p>Accepts decimal, hexadecimal, and octal numbers if property is String.
     *
     * @param name         property name.
     * @param defaultValue default value to use when property omitted.
     * @return numeric property value.
     * @throws scriptella.configuration.ConfigurationException
     *          if parsing failed.
     * @see Long#decode(String)
     */
    public Number getNumberProperty(String name, Number defaultValue) throws ConfigurationException {
        Object v = properties.get(name);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number) {
            return ((Number) v);
        }
        String s = v.toString().trim();
        if (s.length() == 0) {
            return defaultValue;
        }

        //For now we do not support doubles etc.
        try {
            return Long.decode(s);
        } catch (NumberFormatException e) {
            throw new ConfigurationException(name + " property must be integer.");
        }
    }


    /**
     * Parses property value as boolean flag. Default value is <code>false</code>.
     *
     * @param name property name.
     * @return boolean property value.
     * @throws scriptella.configuration.ConfigurationException
     *          if property has unrecognized value.
     * @see #getBooleanProperty(String,boolean)
     */
    public boolean getBooleanProperty(String name) throws ConfigurationException {
        return getBooleanProperty(name, false);
    }

    /**
     * Parses property value as a boolean flag.
     *
     * @param name         property name.
     * @param defaultValue default value to use if connection has no such property.
     * @return boolean property value.
     * @throws ConfigurationException if property has unrecognized value.
     */
    public boolean getBooleanProperty(String name, boolean defaultValue) throws ConfigurationException {
        Object a = getProperty(name);
        if (a == null) {
            return defaultValue;
        }
        if (a instanceof Boolean) {
            return (Boolean) a;
        }
        if (a instanceof Number) {
            return ((Number) a).intValue() > 0;
        }
        String s = a.toString().trim();

        if ("true".equalsIgnoreCase(s) || "1".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s)) {
            return true;
        }

        if ("false".equalsIgnoreCase(s) || "0".equalsIgnoreCase(s) || "off".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s)) {
            return false;
        }
        throw new ConfigurationException("Unrecognized boolean property value " + a);
    }

    /**
     * Parses property value as a charset encoding name.
     *
     * @param name property name.
     * @return value of the property or null if connection has no such property.
     * @throws ConfigurationException if charset name is unsupported.
     */
    public String getCharsetProperty(String name) throws ConfigurationException {
        Object cs = getProperty(name);
        if (cs == null) {
            return null;
        }
        if (cs instanceof Charset) {
            return ((Charset) cs).name();
        }
        String enc = cs.toString().trim();
        if (!Charset.isSupported(enc)) {
            throw new ConfigurationException("Specified encoding " + enc + " is not supported. Supported encodings are " + Charset.availableCharsets().keySet());
        }
        return enc;
    }

    /**
     * Parses property value as URL parameter.
     * <p>Relative URIs are resolved using a script file location as a base.
     *
     * @param name property name
     * @return value of the property or null if connection has no such property.
     * @throws ConfigurationException if URL is malformed.
     */
    public URL getUrlProperty(String name) throws ConfigurationException {
        Object u = getProperty(name);
        if (u == null) {
            return null;
        }
        if (u instanceof URL) {
            return (URL) u;
        }
        try {
            if (u instanceof URI) {
                URI uri = (URI) u;
                return uri.toURL();
            }
            if (u instanceof File) {
                File f = (File) u;
                return IOUtils.toUrl(f);
            }
        } catch (MalformedURLException e) {
            ExceptionUtils.ignoreThrowable(e);
            //If malformed URL try to use the toString resolving
        }

        try {
            String uri = u.toString().trim();
            return getContext().resolve(uri);
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Specified URL " + u + " is malformed");
        }

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
     * Convenience method which parses URL string, extracts a query string and returns a map of URL query parameters.
     * <p>The query is assumed to have the following syntax: ?param=value&param2=value&param3
     * <p>If parameter is declared twice, only the last value is returned in a map.
     * If parameter has only a key, but no value part than the returned map would contain key=key mapping, e.g.
     * jdbc:url?readonly produces a map of 1 entry <code>readonly=readonly</code>.
     * <p>The drivers may use this method to support overriding connection parameters in an URL string.
     *
     * @return map of parsed parameters.
     */
    public Map<String, String> getUrlQueryMap() {
        if (StringUtils.isEmpty(url)) {
            return Collections.emptyMap();
        }
        int lastInd = url.indexOf('?') + 1;
        if (lastInd > 0) {
            Map<String, String> map = new HashMap<String, String>();
            final int urlLength = url.length();
            do {
                int i = url.indexOf('&', lastInd);
                if (i < 0) {
                    i = urlLength;
                }
                String keyValue = url.substring(lastInd, i).trim();
                if (!StringUtils.isEmpty(keyValue)) {
                    int eqPos = keyValue.indexOf('=');
                    if (eqPos > 0) { //If '=' present, split key and value
                        String key = keyValue.substring(0, eqPos).trim();
                        String value = keyValue.substring(eqPos + 1).trim();
                        map.put(key, value);
                    } else { //otherwise use key=value=keyValue
                        map.put(keyValue, keyValue);
                    }
                }
                lastInd = i + 1;
            } while (lastInd < urlLength);
            return map;
        } else {
            return Collections.emptyMap();
        }


    }

    /**
     * Returns the url property resolved relative to a script location.
     *
     * @throws ConfigurationException if connection URL is malformed or null.
     * @return resolved URL.
     */
    public URL getResolvedUrl() throws ConfigurationException {
        if (url == null) {
            throw new ConfigurationException("URL connection attribute is requred");
        }
        try {
            return getContext().resolve(url);
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Specified connection URL " + url + " is malformed");
        }
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
     *
     * @return script context.
     */
    public DriverContext getContext() {
        return context;
    }

    public String toString() {
        return "ConnectionParameters{" + "properties=" + properties + ", url='" + url + '\'' + ", user='" + user + '\'' + ", password='" + password + '\'' + ", schema='" + schema + '\'' + ", catalog='" + catalog + '\'' + '}';
    }
}
