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
package scriptella.execution;

import scriptella.core.Session;
import scriptella.interactive.ProgressCallback;
import scriptella.spi.DriverContext;
import scriptella.spi.ParametersCallback;
import scriptella.util.IOUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
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
    private ParametersCallback properties;
    private URL baseURL;
    private ExecutionStatisticsBuilder statisticsBuilder;
    private Map<String, Object> globalVariables;
    Session session; //Connections related stuff is here

    public EtlContext() {
        this(true);
    }

    public EtlContext(boolean collectStatistics) {
        statisticsBuilder = collectStatistics?new ExecutionStatisticsBuilder(): new SilentExecutionStatisticsBuilder();
        globalVariables = new HashMap<String, Object>();
    }

    public Object getParameter(final String name) {
        return properties.getParameter(name);
    }

    public ProgressCallback getProgressCallback() {
        return progressCallback;
    }

    void setProgressCallback(final ProgressCallback progressCallback) {
        this.progressCallback = progressCallback;
    }

    /**
     * Sets configuration properties for this context.
     * @param properties configuration properties.
     */
    void setProperties(ParametersCallback properties) {
        this.properties = properties;
    }


    public URL getScriptFileURL() {
        return baseURL;
    }

    void setBaseURL(final URL baseURL) {
        this.baseURL = baseURL;
    }

    public URL resolve(final String uri) throws MalformedURLException {
        return IOUtils.resolve(baseURL, uri);
    }

    public Map<String, Object> getGlobalVariables() {
        return globalVariables;
    }

    public ExecutionStatisticsBuilder getStatisticsBuilder() {
        return statisticsBuilder;
    }

    void setStatisticsBuilder(ExecutionStatisticsBuilder statisticsBuilder) {
        this.statisticsBuilder = statisticsBuilder;
    }

    public Session getSession() {
        return session;
    }

}
