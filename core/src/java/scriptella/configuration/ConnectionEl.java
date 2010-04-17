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
package scriptella.configuration;

import java.util.Map;

/**
 * Connection configuration element.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConnectionEl extends XmlConfigurableBase {
    private String id;
    private String url;
    private String driver;
    private String user;
    private String password;
    private String catalog;
    private String schema;
    private String classpath;
    private PropertiesEl properties;
    private boolean lazyInit;

    public ConnectionEl() {
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(final String driver) {
        this.driver = driver;
    }

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(final String catalog) {
        this.catalog = catalog;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(final String schema) {
        this.schema = schema;
    }

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public boolean isLazyInit() {
        return lazyInit;
    }

    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
    }

    /**
     * @return Map of properties for this connection.
     */
    public Map<String, ?> getProperties() {
        return properties.getMap();
    }


    public void configure(final XmlElement element) {
        setProperty(element, "id");
        setProperty(element, "url");
        setRequiredProperty(element, "driver");
        setProperty(element, "user");
        setProperty(element, "password");
        setProperty(element, "catalog");
        setProperty(element, "schema");
        setProperty(element, "classpath");
        lazyInit = element.getBooleanAttribute("lazy-init", false);
        setProperty(element, "classpath");
        properties = new PropertiesEl(element);
    }

    public String toString() {
        StringBuilder res = new StringBuilder("Connection{driver='").append(driver).append('\'');

        res.append("properties=").append(properties);
        if (classpath != null) {
            res.append(", classpath='").append(classpath).append('\'');
        }
        if (schema != null) {
            res.append(", schema='").append(schema).append('\'');
        }
        if (catalog != null) {
            res.append(", catalog='").append(catalog).append('\'');
        }
        if (password != null) {
            res.append(", password='").append(password).append('\'');
        }
        if (user != null) {
            res.append(", user='").append(user).append('\'');
        }
        if (url != null) {
            res.append(", url='").append(url).append('\'');
        }
        if (id != null) {
            res.append(", id='").append(id).append('\'' + '}');
        }

        return res.toString();
    }
}
