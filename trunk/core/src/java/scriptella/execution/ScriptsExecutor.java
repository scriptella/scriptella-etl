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
import scriptella.interactive.ProgressCallback;
import scriptella.interactive.ProgressIndicator;
import scriptella.sql.SQLEngine;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptsExecutor {
    private static final Logger LOG = Logger.getLogger(ScriptsExecutor.class.getName());
    private ConfigurationEl configuration;
    private Map<String, String> externalProperties = new LinkedHashMap<String, String>();

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
            ctx.sqlEngine.rollback();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unable to rollback script", e);
        }
    }

    void commitAll(final ScriptsContext ctx) {
        ctx.sqlEngine.commit();
    }

    void closeAll(final ScriptsContext ctx) {
        ctx.sqlEngine.close();
    }

    private void execute(final ScriptsContext ctx) {
        final ProgressCallback oldProgress = ctx.getProgressCallback();

        final ProgressCallback p = oldProgress.fork(85, 100);
        final ProgressCallback p2 = p.fork(100);
        ctx.setProgressCallback(p2);
        ctx.sqlEngine.execute(ctx);
        p.complete();
        ctx.setProgressCallback(oldProgress);
    }

    protected ScriptsContext prepare(final ProgressIndicator indicator) {
        ScriptsContext ctx = new ScriptsContext();
        ctx.setBaseURL(configuration.getDocumentUrl());
        ctx.sqlEngine = new SQLEngine();
        ctx.setProgressCallback(new ProgressCallback(100, indicator));

        final ProgressCallback progress = ctx.getProgressCallback();
        progress.step(1, "Initializing properties");

        if (externalProperties != null) {
            ctx.addProperties(externalProperties);
        }

        ctx.addProperties(configuration.getProperties());
        ctx.setProgressCallback(progress.fork(9, 100));
        ctx.sqlEngine.init(configuration, ctx);
        ctx.getProgressCallback().complete();
        ctx.setProgressCallback(progress); //Restoring

        return ctx;
    }

    public Map<String, String> getExternalProperties() {
        return externalProperties;
    }

    /**
     * Sets additional properties.
     * <p>Intended for integration with other systems like ant.
     *
     * @param externalProperties
     */
    public void setExternalProperties(final Map<?, ?> externalProperties) {
        this.externalProperties = new HashMap<String, String>((Map<String, String>) externalProperties);
    }
}
