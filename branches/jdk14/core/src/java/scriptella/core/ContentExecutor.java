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
package scriptella.core;

import scriptella.configuration.ContentEl;
import scriptella.configuration.Location;
import scriptella.configuration.ScriptingElement;
import scriptella.spi.DialectIdentifier;
import scriptella.spi.Resource;

/**
 * Base class for Script/Query executors.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class ContentExecutor<T extends ScriptingElement> implements ExecutableElement {
    private Resource cachedContent; //initialized on the first execution
    private T element;

    /**
     * Initializes and stores reference to script element.
     *
     * @param scriptingElement query or script.
     */
    protected ContentExecutor(T scriptingElement) {
        this.element = scriptingElement;
    }

    /**
     * Returns scripting element specified in {@link #ContentExecutor constructor}
     *
     * @return scripting element
     */
    public T getElement() {
        return element;
    }

    /**
     * Returns content satisfying dialect information.
     * <p>This method caches contents based on dialects.
     * For now we assume dialectIdentifier is constant for element.
     *
     * @param dialectIdentifier dialect identifier.
     * @return content for dialect. Not null.
     */
    public Resource getContent(DialectIdentifier dialectIdentifier) {
        if (cachedContent == null) {
            cachedContent = element.getDialectContent(dialectIdentifier);
        }
        if (cachedContent == null) { //avoid double initialization
            cachedContent = ContentEl.NULL_CONTENT;
        }
        return cachedContent;

    }


    /**
     * A short for {@link #getElement()}.{@link scriptella.configuration.ScriptingElement#getLocation() getLocation()}
     */
    public Location getLocation() {
        return getElement().getLocation();
    }
}
