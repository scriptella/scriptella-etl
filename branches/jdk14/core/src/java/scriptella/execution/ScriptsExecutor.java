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

import scriptella.configuration.ConfigurationEl;
import scriptella.configuration.ConfigurationFactory;
import scriptella.core.Session;
import scriptella.interactive.ProgressCallback;
import scriptella.interactive.ProgressIndicator;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Executor for script files.
 * <p>Use {@link ConfigurationFactory} to parse script files and to configure
 * the executor.
 * <p>The usage scenario of this class may be described using the following steps:
 * <ul>
 * <li>{@link #ScriptsExecutor(ConfigurationEl)} Create an instance of this class
 * and pass a {@link scriptella.configuration.ConfigurationFactory#createConfiguration() script file configuration} .
 * <li>Optionally {@link #setExternalProperties(java.util.Map) set external properties}.
 * <li>{@link #execute() Execute} the script
 * </ul>
 * </pre></code>
 * <p>Additionally simplified helper methods are declared in this class:
 * <ul>
 * <li>{@link #newExecutor(java.net.URL)}
 * <li>{@link #newExecutor(java.net.URL, java.util.Map)}
 * </ul>
 *
 *
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptsExecutor {
    private static final Logger LOG = Logger.getLogger(ScriptsExecutor.class.getName());
    private ConfigurationEl configuration;
    private Map<String, String> externalProperties;

    public ScriptsExecutor() {
    }

    public ScriptsExecutor(ConfigurationEl configuration) {
        this.configuration = configuration;
    }

    public ConfigurationEl getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final ConfigurationEl configuration) {
        this.configuration = configuration;
    }

    public ExecutionStatistics execute() throws ScriptsExecutorException {
        return execute((ProgressIndicator) null);
    }

    public ExecutionStatistics execute(final ProgressIndicator indicator)
            throws ScriptsExecutorException {
        ScriptsContext ctx = null;

        try {
            ctx = prepare(indicator);
            execute(ctx);
            ctx.getProgressCallback().step(5, "Commiting transactions");
            commitAll(ctx);
        } catch (Throwable e) {
            if (ctx != null) {
                rollbackAll(ctx);
            }
            throw new ScriptsExecutorException(e);
        } finally {
            if (ctx != null) {
                closeAll(ctx);
                ctx.getProgressCallback().complete();
            }
        }

        return ctx.getStatisticsBuilder().getStatistics();
    }

    void rollbackAll(final ScriptsContext ctx) {
        try {
            ctx.session.rollback();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unable to rollback script", e);
        }
    }

    void commitAll(final ScriptsContext ctx) {
        ctx.session.commit();
    }

    void closeAll(final ScriptsContext ctx) {
        ctx.session.close();
    }

    private void execute(final ScriptsContext ctx) {
        final ProgressCallback oldProgress = ctx.getProgressCallback();

        final ProgressCallback p = oldProgress.fork(85, 100);
        final ProgressCallback p2 = p.fork(100);
        ctx.setProgressCallback(p2);
        ctx.session.execute(ctx);
        p.complete();
        ctx.setProgressCallback(oldProgress);
    }

    protected ScriptsContext prepare(final ProgressIndicator indicator) {
        ScriptsContext ctx = new ScriptsContext();
        ctx.setBaseURL(configuration.getDocumentUrl());
        ctx.session = new Session();
        ctx.setProgressCallback(new ProgressCallback(100, indicator));

        final ProgressCallback progress = ctx.getProgressCallback();
        progress.step(1, "Initializing properties");

        if (externalProperties != null) {
            ctx.addProperties(externalProperties);
        }

        ctx.addProperties(configuration.getProperties());
        ctx.setProgressCallback(progress.fork(9, 100));
        ctx.session.init(configuration, ctx);
        ctx.getProgressCallback().complete();
        ctx.setProgressCallback(progress); //Restoring

        return ctx;
    }

    public Map<String, String> getExternalProperties() {
        return externalProperties;
    }

    /**
     * Sets additional properties.
     * <p>External properties takes precedence over properties specified
     * in scriptella &lt;properties&gt; element.
     * <p>Intended for integration with other systems like ant.
     *
     * @param externalProperties
     */
    public void setExternalProperties(final Map<?, ?> externalProperties) {
        this.externalProperties = new LinkedHashMap<String, String>((Map<String, String>) externalProperties);
    }

    /**
     * Helper method to create a new ScriptExecutor for specified script URL.
     * <p>Calls {@link #newExecutor(java.net.URL, java.util.Map)}.
     * @param scriptFileUrl URL of script file.
     * @return configured instance of script executor.
     */
    public static ScriptsExecutor newExecutor(final URL scriptFileUrl) {
        return newExecutor(scriptFileUrl, null);
    }

    /**
     * Helper method to create a new ScriptExecutor for specified script URL.
     * @param scriptFileUrl URL of script file.
     * @param externalProperties see {@link #setExternalProperties(java.util.Map)}
     * @return configured instance of script executor.
     * @see ConfigurationFactory
     */
    public static ScriptsExecutor newExecutor(final URL scriptFileUrl, final Map<String,String> externalProperties) {
        ConfigurationFactory cf = new ConfigurationFactory();
        cf.setResourceURL(scriptFileUrl);
        ConfigurationEl c = cf.createConfiguration();
        ScriptsExecutor se = new ScriptsExecutor(c);
        if (externalProperties!=null) {
            se.setExternalProperties(externalProperties);
        }
        return se;
    }

}
