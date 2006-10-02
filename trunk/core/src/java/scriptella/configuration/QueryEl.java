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

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class QueryEl extends ScriptingElement {
    public static final String TAG_NAME = "query";
    private List<ScriptingElement> childScriptinglElements;

    public QueryEl() {
    }

    public QueryEl(XmlElement element) {
        configure(element);
    }


    public List<ScriptingElement> getChildScriptinglElements() {
        return childScriptinglElements;
    }

    public void setChildScriptinglElements(
            final List<ScriptingElement> childScriptinglElements) {
        this.childScriptinglElements = childScriptinglElements;
    }

    static List<ScriptingElement> loadScriptingElements(final XmlElement element) {
        final List<XmlElement> elements = element.getChildren(new HashSet<String>(
                Arrays.asList(QueryEl.TAG_NAME, ScriptEl.TAG_NAME)));
        List<ScriptingElement> scripts = new ArrayList<ScriptingElement>(elements.size());

        for (XmlElement xmlElement : elements) {
            final Element e = xmlElement.getElement();

            if (ScriptEl.TAG_NAME.equals(e.getTagName())) {
                final ScriptEl s = new ScriptEl(xmlElement);
                scripts.add(s);
            } else if (QueryEl.TAG_NAME.equals(e.getTagName())) {
                final QueryEl q = new QueryEl(xmlElement);
                scripts.add(q);
            }
        }

        return scripts;
    }

    public void configure(final XmlElement element) {
        super.configure(element);
        setChildScriptinglElements(loadScriptingElements(element));
        setLocation(element, "queries");
    }
}
