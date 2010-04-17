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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import scriptella.spi.DialectIdentifier;
import scriptella.spi.Resource;
import scriptella.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents dialect based content used inside query/script/onerror elements.
 * <p>When the DOM is traversed the internal representation model is built.
 * This model contains enough info to speed up selection based on a requested dialect identifier.
 * The following example demonstrates the algorithm
 * using pseudo markup:
 * <pre>
 * AAA[Dialect1 111]BBB[Dialect2 222]
 * </pre>
 * The returned text for default Dialect is: AAABBB
 * <br> The returned text for Dialect1 is: AAA111BBB
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DialectBasedContentEl extends XmlConfigurableBase {
    private List<Dialect> dialects;

    public DialectBasedContentEl() {
    }

    public DialectBasedContentEl(final XmlElement element) {
        configure(element);
    }

    public void configure(final XmlElement element) {
        Dialect defaultDialect = null;
        dialects = new ArrayList<Dialect>();
        //iterate through the child nodes of this element
        for (Node node = element.getElement().getFirstChild(); node != null; node = node.getNextSibling()) {
            if (isDialectElement(node)) {
                if (defaultDialect != null) {
                    dialects.add(defaultDialect);
                    defaultDialect = null;
                }
                Dialect d = new Dialect();
                d.configure(new XmlElement((Element) node, element));
                dialects.add(d);
            } else {
                //Try to convert the node to resource if possible
                Resource resource = ContentEl.asResource(element, node);
                //If it's a text or include
                if (resource != null) {
                    //check if we have default dialect instance
                    if (defaultDialect == null) {
                        //if no - create one
                        defaultDialect = new Dialect();
                        defaultDialect.configureDefault(element);
                    }
                    //append a resource to default dialect
                    defaultDialect.contentEl.append(resource);
                }
            }
        }
        if (defaultDialect != null) {
            dialects.add(defaultDialect); //
        }

    }

    private static boolean isDialectElement(Node node) {
        return node instanceof Element && "dialect".equals(node.getNodeName());
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
                    result.setLocation(getLocation());
                }
                result.merge(d.getContentEl());
            }
        }
        return result;
    }

    /**
     * For testing purposes
     *
     * @return internal list of dialects.
     */
    List<Dialect> getDialects() {
        return dialects;
    }

    static class Dialect extends XmlConfigurableBase {
        private Pattern name;
        private Pattern version;
        private boolean exclude;
        private ContentEl contentEl;

        public Pattern getName() {
            return name;
        }

        public void setName(final Pattern name) {
            this.name = name;
        }

        public Pattern getVersion() {
            return version;
        }

        public void setVersion(final Pattern version) {
            this.version = version;
        }


        public boolean isExclude() {
            return exclude;
        }

        public void setExclude(boolean exclude) {
            this.exclude = exclude;
        }

        public ContentEl getContentEl() {
            return contentEl;
        }

        public void configure(final XmlElement element) {
            setPatternProperty(element, "name");
            setPatternProperty(element, "version");
            exclude=element.getBooleanAttribute("exclude", false);
            contentEl = new ContentEl(element);
            setLocation(element);
        }

        /**
         * Configures default dialect.
         *
         * @param parent parent element.
         */
        public void configureDefault(final XmlElement parent) {
            setLocation(parent);
            contentEl = new ContentEl();
        }


        boolean matches(final DialectIdentifier id) {
            if (id == null) { //if db has no dialect identifier
                //return true only if we have no specified restrictions
                return name == null && version == null;
            }

            String idName = StringUtils.nullsafeToString(id.getName());
            String idVersion = StringUtils.nullsafeToString(id.getVersion());
            //Substring matching is used for names. Versions are matched entirely
            boolean matches = ((name == null) ||
                    name.matcher(idName).find()) && ((version == null) ||
                    version.matcher(idVersion).matches());
            return exclude?!matches:matches;
        }

        public String toString() {
            return "Dialect{" + "name=" + name + ", version=" +
                    version + ", exclude="+exclude+", contentEl=" + contentEl + "}";
        }
    }

}
