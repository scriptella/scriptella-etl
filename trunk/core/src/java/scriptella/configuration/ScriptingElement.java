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


/**
 * Base class for queries and scripts.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class ScriptingElement extends XMLConfigurableBase {
    private String connectionId;
    private String ifExpr;
    private DialectBasedContentEl contentEl;

    protected ScriptingElement() {
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(final String connectionId) {
        this.connectionId = connectionId;
    }

    public Location getLocation() {
        return super.getLocation();
    }

    public String getIf() {
        return ifExpr;
    }

    public void setIf(final String ifExpr) {
        this.ifExpr = ifExpr;
    }

    public ContentEl getContent() {
        return contentEl.getContent(null);
    }


    public ContentEl getDialectContent(DialectIdentifier id) {
        return contentEl.getContent(id);
    }

    public void configure(final XMLElement element) {
        setProperty(element, "connection-id", "connectionId");
        setProperty(element, "if");
        contentEl = new DialectBasedContentEl(element);
    }


    public String toString() {
        return "connectionId='" + connectionId + '\'' +
                ", location=" + getLocation() +
                ", ifExpr='" + ifExpr + '\'';

    }
}
