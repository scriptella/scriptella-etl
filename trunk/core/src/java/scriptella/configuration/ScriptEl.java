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

/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptEl extends SQLBasedElement {
    public static final String TAG_NAME = "script";
    private boolean newTx;

    public ScriptEl() {
    }

    public ScriptEl(XMLElement element) {
        configure(element);
    }

    public boolean isNewTx() {
        return newTx;
    }

    public void setNewTx(final boolean newTx) {
        this.newTx = newTx;
    }

    public void configure(final XMLElement element) {
        super.configure(element);
        newTx = element.getBooleanProperty("new-tx", false);
        location = new Location(element.getXPath(), "scripts");
    }

    public String toString() {
        return "ScriptEl{" + "dialects=" + dialects + ", newTx=" + newTx + "}";
    }
}
