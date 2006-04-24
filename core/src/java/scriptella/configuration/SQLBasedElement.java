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
package scriptella.configuration;

import scriptella.sql.DialectIdentifier;

import java.util.List;
import java.util.regex.Pattern;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class SQLBasedElement extends XMLConfigurableBase {
    private String connectionId;
    protected List<Dialect> dialects;
    protected Location location;
    private String ifExpr;

    protected SQLBasedElement() {
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(final String connectionId) {
        this.connectionId = connectionId;
    }

    /**
     * This method returns content for specified dialect id or null - if script doesn't support this dialect.
     *
     * @param id dialect identifier. null if any dialect.
     * @return content for specified dialect id or null - if script doesn't support this dialect.
     */
    public ContentEl getContent(final DialectIdentifier id) {
        ContentEl result=null;
        for (Dialect d : dialects) {
            if (d.matches(id)) {
                if (result==null) {
                    result = new ContentEl();
                }
                result.merge(d.getContentEl());
            }
        }
        return result;
    }

    public void addDialects(final Dialect dialect) {
        dialects.add(dialect);
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(final Location location) {
        this.location = location;
    }

    public String getIf() {
        return ifExpr;
    }

    public void setIf(final String ifExpr) {
        this.ifExpr = ifExpr;
    }

    public void configure(final XMLElement element) {
        setProperty(element, "connection-id", "connectionId");
        setProperty(element, "if");
        //The following code loads nested dialect elements
        dialects = load(element.getChildren("dialect"), Dialect.class);
        //Use element text as a default dialect
        Dialect d = new Dialect();
        d.configure(element);
        dialects.add(d);
    }

    public static class Dialect extends XMLConfigurableBase {
        private Pattern database;
        private Pattern version;
        private ContentEl contentEl;

        public Pattern getDatabase() {
            return database;
        }

        public void setDatabase(final Pattern database) {
            this.database = database;
        }

        public Pattern getVersion() {
            return version;
        }

        public void setVersion(final Pattern version) {
            this.version = version;
        }

        public ContentEl getContentEl() {
            return contentEl;
        }

        public void setContentEl(final ContentEl contentEl) {
            this.contentEl = contentEl;
        }

        public void configure(final XMLElement element) {
            String db = element.getAttribute("database");
            database = null;

            if ((db != null) && (db.length() > 0)) {
                try {
                    database = Pattern.compile(db, Pattern.CASE_INSENSITIVE);
                } catch (Exception e) {
                    throw new ConfigurationException("Unable to configuration database attribute",
                            e, element);
                }
            }

            String v = element.getAttribute("version");
            version = null;

            if ((v != null) && (v.length() > 0)) {
                try {
                    version = Pattern.compile(v, Pattern.CASE_INSENSITIVE);
                } catch (Exception e) {
                    throw new ConfigurationException("Unable to configuration version attribute",
                            e, element);
                }
            }

            contentEl = new ContentEl(element);
        }

        boolean matches(final DialectIdentifier id) {
            if (id == null) {
                return true;
            }

            if ((database != null) &&
                    !database.matcher(id.getName()).matches()) {
                return false;
            }

            if ((version != null) &&
                    !version.matcher(id.getVersion()).matches()) {
                return false;
            }

            return true;
        }

        public String toString() {
            return "Dialect{" + "database=" + database + ", version=" +
                    version + ", contentEl=" + contentEl + "}";
        }
    }
}
