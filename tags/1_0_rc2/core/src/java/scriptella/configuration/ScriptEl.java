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

import java.util.List;

/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptEl extends ScriptingElement {
    public static final String TAG_NAME = "script";
    private boolean newTx;
    protected List<OnErrorEl> onerrors;

    public ScriptEl(XmlElement element, ScriptingElement parent) {
        super(parent);
        configure(element);
    }

    public boolean isNewTx() {
        return newTx;
    }

    public void setNewTx(final boolean newTx) {
        this.newTx = newTx;
    }

    public List<OnErrorEl> getOnerrorElements() {
        return onerrors;
    }

    public void setOnerrorElements(List<OnErrorEl> list) {
        onerrors = list;
    }

    public void configure(final XmlElement element) {
        super.configure(element);
        setNewTx(element.getBooleanAttribute("new-tx", false));
        //The following code loads nested onerror elements
        setOnerrorElements(load(element.getChildren("onerror"), OnErrorEl.class));
    }

    public String toString() {
        return "ScriptEl{" + super.toString() + ", newTx=" + newTx + "}";
    }
}
