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
package scriptella.tools.ant;

import org.apache.tools.ant.Task;
import scriptella.interactive.LoggingConfigurer;
import scriptella.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Base class for Scriptella ETL Ant tasks.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlTaskBase extends Task {
    private boolean inheritAll = true;
    private boolean debug;
    private boolean quiet;
    private AntHandler handler; //Optional Ant handler used for logging

    /**
     * Setter for inheritAll property.
     *
     * @param inheritAll if <code>true</code> pass all project properties to Scriptella. Default value if <code>true</code>.
     */
    public void setInheritAll(boolean inheritAll) {
        this.inheritAll = inheritAll;
    }


    /**
     * Getter for inheritAll property.
     *
     * @return true if pass all project properties to Scriptella. Default value if <code>true</code>.
     */
    public boolean isInheritAll() {
        return inheritAll;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }


    /**
     * Returns a map of properties to pass into Scriptella.
     * <p>If {@link #isInheritAll() inheritAll} property is true (the default),
     * the set of project properties is returned, otherwise only system properties are passed.
     *
     * @return map of properties (name->value).
     */
    @SuppressWarnings("unchecked")
    protected Map<String, ?> getProperties() {
        Map<String, Object> result = new HashMap<String, Object>();
        if (inheritAll) { //inherit ant properties - not supported in forked mode yet
            result.putAll(getProject().getProperties());
        } else {
            result.putAll(CollectionUtils.asMap(System.getProperties()));
        }
        return result;
    }

    /**
     * Configures Scriptella JUL loggers to use Ant logging.
     */
    protected void setupLogging() {
        if (handler != null) {
            resetLogging();
        }
        handler = new AntHandler(getProject(), debug);
        handler.setLevel(Level.INFO);
        if (debug) {
            handler.setLevel(Level.FINE);
        }
        if (quiet) {
            handler.setLevel(Level.WARNING);
        }
        LoggingConfigurer.configure(handler);
    }

    /**
     * Resets JUL back to the original state.
     */
    protected void resetLogging() {
        if (handler != null) {
            LoggingConfigurer.remove(handler);
        }
    }


}
