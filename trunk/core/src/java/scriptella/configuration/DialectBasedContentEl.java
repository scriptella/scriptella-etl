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

import scriptella.spi.DialectIdentifier;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents dialect based content used inside query/script/onerror elements.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DialectBasedContentEl extends XMLConfigurableBase {
    protected List<Dialect> dialects;

    public DialectBasedContentEl() {
    }

    public DialectBasedContentEl(final XMLElement element) {
        configure(element);
    }

    public void configure(final XMLElement element) {
        //todo modify parsing to create dialect tree according to position in XML
        //The following code loads nested dialect elements
        dialects = load(element.getChildren("dialect"), Dialect.class);
        //Use element text as a default dialect
        Dialect d = new Dialect();
        d.configure(element);
        dialects.add(d);

    }

    /**
     * This method returns content for specified dialect id or null - if script doesn't support this dialect.
     *
     * @param id dialect identifier. null if any dialect.
     * @return content for specified dialect id or null - if script doesn't support this dialect.
     */
    public ContentEl getContent(final DialectIdentifier id) {
        ContentEl result = null;
        for (Dialect d : dialects) {
            if (d.matches(id)) {
                if (result == null) {
                    result = new ContentEl();
                }
                result.merge(d.getContentEl());
            }
        }
        return result;
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
            setPatternProperty(element, "database");
            setPatternProperty(element, "version");

            contentEl = new ContentEl(element);
        }

        boolean matches(final DialectIdentifier id) {
            if (id == null) { //if db has no dialect identifier
                //return true only if we have no specified restrictions
                return database == null && version == null;
            }

            if ((database != null) &&
                    !database.matcher(id.getName()).matches()) {
                return false;
            }

            return !((version != null) &&
                    !version.matcher(id.getVersion()).matches());

        }

        public String toString() {
            return "Dialect{" + "database=" + database + ", version=" +
                    version + ", contentEl=" + contentEl + "}";
        }
    }

}
