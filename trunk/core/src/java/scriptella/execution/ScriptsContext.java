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
package scriptella.execution;

import scriptella.expressions.ParametersCallback;
import scriptella.expressions.PropertiesSubstitutor;
import scriptella.expressions.ThisParameter;
import scriptella.interactive.ProgressCallback;
import scriptella.sql.SQLEngine;
import scriptella.util.PropertiesMap;

import java.net.URL;
import java.util.Map;


/**
 * Execution context for script.
 * <p/>
 * This class contains global data for executed elements.
 * <br><b>Note:</b> Execution context is not intended to change its state during sql elements
 * execution as opposed to {@link scriptella.sql.SQLContext}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptsContext implements ParametersCallback {
    private ProgressCallback progressCallback;
    private Map<String, String> properties = new PropertiesMap();
    private URL baseURL;
    private ExecutionStatisticsBuilder statisticsBuilder = new ExecutionStatisticsBuilder();
    SQLEngine sqlEngine; //SQL stuff is here
    private final PropertiesSubstitutor propertiesSubstitutor = new PropertiesSubstitutor();
    private ThisParameter thisParameter = new ThisParameter(this);

    public Object getParameter(final String name) {
        if (ThisParameter.NAME.equals(name)) {
            return thisParameter;
        }

        return properties.get(name);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public ProgressCallback getProgressCallback() {
        return progressCallback;
    }

    void setProgressCallback(final ProgressCallback progressCallback) {
        this.progressCallback = progressCallback;
    }

    /**
     * Adds properties and expands their values by evaluating expressions and property references.
     *
     * @param properties properties to add to scripts context.
     * @see #substituteProperties(String)
     */
    void addProperties(final Map<String, String> properties) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            this.properties.put(entry.getKey(), substituteProperties(entry.getValue()));
        }
    }

    public URL getBaseURL() {
        return baseURL;
    }

    void setBaseURL(final URL baseURL) {
        this.baseURL = baseURL;
    }

    public ExecutionStatisticsBuilder getStatisticsBuilder() {
        return statisticsBuilder;
    }

    void setStatisticsBuilder(
            final ExecutionStatisticsBuilder statisticsBuilder) {
        this.statisticsBuilder = statisticsBuilder;
    }

    public SQLEngine getSqlEngine() {
        return sqlEngine;
    }

    void setSqlEngine(final SQLEngine sqlEngine) {
        this.sqlEngine = sqlEngine;
    }

    public String substituteProperties(final String s) {
        return propertiesSubstitutor.substitute(s, this);
    }
}
