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

import scriptella.core.Session;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.interactive.ProgressCallback;
import scriptella.spi.DriverContext;
import scriptella.spi.ParametersCallback;
import scriptella.util.PropertiesMap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;


/**
 * Execution context for script.
 * <p/>
 * This class contains global data for executed elements.
 * <br><b>Note:</b> Execution context is not intended to change its state during sql elements
 * execution as opposed to {@link scriptella.core.DynamicContext}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlContext implements ParametersCallback, DriverContext {
    private ProgressCallback progressCallback;
    private Map<String, String> properties = new PropertiesMap();
    private URL baseURL;
    private ExecutionStatisticsBuilder statisticsBuilder = new ExecutionStatisticsBuilder();
    Session session; //Connections related stuff is here
    private final PropertiesSubstitutor propertiesSubstitutor = new PropertiesSubstitutor(this);

    public Object getParameter(final String name) {
        return properties.get(name);
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

    public URL getScriptFileURL() {
        return baseURL;
    }

    void setBaseURL(final URL baseURL) {
        this.baseURL = baseURL;
    }

    public URL resolve(final String uri) throws MalformedURLException {
        return new URL(baseURL, uri);
    }


    public ExecutionStatisticsBuilder getStatisticsBuilder() {
        return statisticsBuilder;
    }

    public Session getSession() {
        return session;
    }

    public String substituteProperties(final String s) {
        return propertiesSubstitutor.substitute(s);
    }

}
