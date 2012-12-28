/*
 * Copyright 2006-2012 The Scriptella Project Team.
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
import scriptella.configuration.IncludeEl;
import scriptella.configuration.Location;
import scriptella.configuration.ScriptingElement;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.Connection;
import scriptella.spi.DialectIdentifier;
import scriptella.spi.Resource;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for Script/Query executors.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
abstract class ContentExecutor<T extends ScriptingElement> implements ExecutableElement {
    private Resource cachedContent; //initialized on the first execution
    private T element;
    private PropertiesSubstitutor contentPropsSubstitutor;
    protected final Logger log = Logger.getLogger(getClass().getName());
    protected final boolean debug = log.isLoggable(Level.FINE);

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


    public final void execute(final DynamicContext ctx) {
        final Connection c = ctx.getConnection();
        final Resource content = getContent(c.getDialectIdentifier());
        if (content == ContentEl.NULL_CONTENT && debug) {
            log.fine("Element " + getLocation() + " has no supported dialects, no content executed.");
        }
        try {
            //Set parameters if content is dynamic
            if (contentPropsSubstitutor != null) { //If initialized
                contentPropsSubstitutor.setParameters(ctx);
            }
            execute(c, content, ctx);
        } finally {
            //Clear the parameters if initialized
            if (contentPropsSubstitutor != null) {
                contentPropsSubstitutor.setParameters(null);
            }

        }
    }

    /**
     * Executes a script or a query resource using the specified connection.
     *
     * @param connection connection to use for element execution.
     * @param resource   query/script content to execute.
     * @param ctx        dynamic context to use for variables, etc.
     */
    protected abstract void execute(final Connection connection, final Resource resource, final DynamicContext ctx);


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
            cachedContent = prepareContent(element.getDialectContent(dialectIdentifier));
        }
        if (cachedContent == null) { //avoid double initialization
            cachedContent = ContentEl.NULL_CONTENT;
        }
        return cachedContent;

    }

    /**
     * A short for {@link #getElement()}.{@link scriptella.configuration.ScriptingElement#getLocation() getLocation()}
     *
     * @return element location.
     */
    public Location getLocation() {
        return getElement().getLocation();
    }

    /**
     * Prepares the content for later execution.
     * <p>If content is dynamic, i.e. contains includes with bind variables, the properties substitutor is specified,
     * otherwise no properties .
     *
     * @param content content to traverse.
     * @return the same content object for convenience only.
     */
    protected ContentEl prepareContent(ContentEl content) {
        if (content == null) {
            return null;
        }
        for (Resource resource : content.getResources()) {
            if (resource instanceof IncludeEl) {
                IncludeEl include = (IncludeEl) resource;
                //Performance optimization: check if href contains properties
                if (PropertiesSubstitutor.hasProperties(include.getHref())) {
                    if (contentPropsSubstitutor == null) { //Lazily initialize the substitutor
                        contentPropsSubstitutor = new PropertiesSubstitutor();
                    }
                    include.setPropertiesSubstitutor(contentPropsSubstitutor);
                }
            }
        }
        return content;
    }
}
