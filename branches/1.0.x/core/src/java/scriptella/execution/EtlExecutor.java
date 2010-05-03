/*
 * Copyright 2006-2009 The Scriptella Project Team.
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
import scriptella.core.SystemException;
import scriptella.core.ThreadSafe;
import scriptella.interactive.ProgressCallback;
import scriptella.interactive.ProgressIndicator;
import scriptella.util.CollectionUtils;
import scriptella.util.IOUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Executor for ETL files.
 * <p>Use {@link ConfigurationFactory} to parse script files and to configure
 * the executor.
 * <p>The usage scenario of this class may be described using the following steps:
 * <ul>
 * <li>{@link #EtlExecutor(scriptella.configuration.ConfigurationEl)} Create an instance of this class
 * and pass a {@link scriptella.configuration.ConfigurationFactory#createConfiguration() script file configuration}.
 * <li>{@link #execute() Execute} the script
 * </ul>
 * </pre></code>
 * <p>Additionally simplified helper methods are declared in this class:
 * <ul>
 * <li>{@link #newExecutor(java.io.File)}
 * <li>{@link #newExecutor(java.net.URL)}
 * <li>{@link #newExecutor(java.net.URL, java.util.Map)}
 * </ul>
 * <p/>
 * <h3>ETL Cancellation</h3>
 * Scriptella execution model relies on a standard Java {@link Thread#interrupt()} mechanism.
 * <p>To interrupt the ETL execution invoke {@link Thread#interrupt()} on a thread
 * which {@link #execute() started} ETL operation. As a part of interruption process
 * the engine tries to roll back all changes made during the ETL operation.
 * <p>{@link java.util.concurrent.ExecutorService} and {@link java.util.concurrent.Future}
 * can also be used to control ETL execution.
 * <h3>Integration with third-party systems</h3>
 * For convenience EtlExecutor implements {@link Runnable} and {@link java.util.concurrent.Callable}.
 * This feature simplifies integration of Scriptella executors with {@link java.util.concurrent.Executors}
 * or other systems like Spring/Quartz etc. It also minimizes application code dependency on Scriptella.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlExecutor implements Runnable, Callable<ExecutionStatistics> {
    private static final Logger LOG = Logger.getLogger(EtlExecutor.class.getName());
    private ConfigurationEl configuration;
    private boolean jmxEnabled;
    private boolean suppressStatistics;

    /**
     * Creates ETL executor.
     */
    public EtlExecutor() {
    }

    /**
     * Creates an ETL executor for specified configuration file.
     *
     * @param configuration ETL configuration.
     */
    public EtlExecutor(ConfigurationEl configuration) {
        this.configuration = configuration;
    }

    /**
     * Returns ETL configuration for this executor.
     *
     * @return ETL configuration.
     */
    public ConfigurationEl getConfiguration() {
        return configuration;
    }

    /**
     * Sets ETL configuration.
     *
     * @param configuration ETL configuration.
     */
    public void setConfiguration(final ConfigurationEl configuration) {
        this.configuration = configuration;
    }


    /**
     * Returns true if monitoring/management via JMX is enabled.
     * <p>If jmxEnabled=true the executor registers MBeans for executed ETL files.
     * The object names of the mbeans have the following form:
     * <code>scriptella: type=etl,url="ETL_FILE_URL"</code>
     *
     * @return true if monitoring/management via JMX is enabled.
     */
    public boolean isJmxEnabled() {
        return jmxEnabled;
    }

    /**
     * Enables or disables ETL monitoring/management via JMX.
     * <p>If jmxEnabled=true the executor registers MBeans for executed ETL files.
     * The object names of the mbeans have the following form:
     * <code>scriptella: type=etl,url="ETL_FILE_URL"</code>
     *
     * @param jmxEnabled true if monitoring/management via JMX is enabled.
     * @see scriptella.execution.JmxEtlManagerMBean
     */
    public void setJmxEnabled(boolean jmxEnabled) {
        this.jmxEnabled = jmxEnabled;
    }

    /**
     * Getter for {@link #setSuppressStatistics(boolean) suppressStatistics} property.
     * @return true if statistics collection is disabled. Default value is false.
     */
    public boolean isSuppressStatistics() {
        return suppressStatistics;
    }

    /**
     * Enables or disables collecting of statistics. Default value is false, which means statistics is collected.
     * <p>Setting this option to <code>true</code> may improve performance in some cases.
     * @param suppressStatistics true if statistics collection should be disabled.
     */
    public void setSuppressStatistics(boolean suppressStatistics) {
        this.suppressStatistics = suppressStatistics;
    }

    /**
     * Executes ETL based on a specified configuration.
     *
     * @return execution statistics for ETL execution.
     * @throws EtlExecutorException if ETL fails.
     * @see #execute(scriptella.interactive.ProgressIndicator)
     */
    @ThreadSafe
    public ExecutionStatistics execute() throws EtlExecutorException {
        return execute((ProgressIndicator) null);
    }

    /**
     * Executes ETL based on a specified configuration.
     *
     * @param indicator progress indicator to use.
     * @return execution statistics for ETL execution.
     * @throws EtlExecutorException if ETL fails.
     */
    @ThreadSafe
    public ExecutionStatistics execute(final ProgressIndicator indicator)
            throws EtlExecutorException {
        EtlContext ctx = null;
        JmxEtlManager etlManager = null;

        try {
            ctx = prepare(indicator);
            if (jmxEnabled) {
                etlManager = new JmxEtlManager(ctx);
                etlManager.register();
            }
            execute(ctx);
            ctx.getProgressCallback().step(5, "Commiting transactions");
            commitAll(ctx);
        } catch (Throwable e) {
            if (ctx != null) {
                rollbackAll(ctx);
            }
            throw new EtlExecutorException(e);
        } finally {
            if (ctx != null) {
                closeAll(ctx);
                ctx.getStatisticsBuilder().etlComplete();
                ctx.getProgressCallback().complete();
            }
            if (etlManager != null) {
                etlManager.unregister();
            }
        }

        return ctx.getStatisticsBuilder().getStatistics();
    }

    void rollbackAll(final EtlContext ctx) {
        try {
            ctx.session.rollback();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unable to rollback script", e);
        }
    }

    void commitAll(final EtlContext ctx) {
        ctx.session.commit();
    }

    void closeAll(final EtlContext ctx) {
        ctx.session.close();
    }

    private void execute(final EtlContext ctx) {
        final ProgressCallback oldProgress = ctx.getProgressCallback();

        final ProgressCallback p = oldProgress.fork(85, 100);
        final ProgressCallback p2 = p.fork(100);
        ctx.setProgressCallback(p2);
        ctx.session.execute(ctx);
        p.complete();
        ctx.setProgressCallback(oldProgress);
    }

    /**
     * Prepares the scripts context.
     *
     * @param indicator progress indicator to use.
     * @return prepared scripts context.
     */
    protected EtlContext prepare(final ProgressIndicator indicator) {
        EtlContext ctx = new EtlContext(!suppressStatistics);
        ctx.getStatisticsBuilder().etlStarted();
        ctx.setBaseURL(configuration.getDocumentUrl());
        ctx.setProgressCallback(new ProgressCallback(100, indicator));

        final ProgressCallback progress = ctx.getProgressCallback();
        progress.step(1, "Initializing properties");
        ctx.setProperties(configuration.getParameters());
        ctx.setProgressCallback(progress.fork(9, 100));
        ctx.session = new Session(configuration, ctx);
        ctx.getProgressCallback().complete();
        ctx.setProgressCallback(progress); //Restoring

        return ctx;
    }

    /**
     * Converts file to URL and invokes {@link #newExecutor(java.net.URL)}.
     *
     * @param scriptFile ETL file.
     * @return configured instance of script executor.
     * @see #newExecutor(java.net.URL)
     */
    public static EtlExecutor newExecutor(final File scriptFile) {
        try {
            return newExecutor(IOUtils.toUrl(scriptFile));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Helper method to create a new ScriptExecutor for specified script URL.
     * <p>Calls {@link #newExecutor(java.net.URL, java.util.Map)} and passes {@link System#getProperties() System properties}
     * as external properties.
     *
     * @param scriptFileUrl URL of script file.
     * @return configured instance of script executor.
     */
    @ThreadSafe
    public static EtlExecutor newExecutor(final URL scriptFileUrl) {
        return newExecutor(scriptFileUrl, CollectionUtils.asMap(System.getProperties()));
    }

    /**
     * Helper method to create a new ScriptExecutor for specified script URL.
     *
     * @param scriptFileUrl      URL of script file.
     * @param externalProperties see {@link ConfigurationFactory#setExternalParameters(java.util.Map)}
     * @return configured instance of script executor.
     * @see ConfigurationFactory
     */
    @ThreadSafe
    public static EtlExecutor newExecutor(final URL scriptFileUrl, final Map<String, ?> externalProperties) {
        ConfigurationFactory cf = new ConfigurationFactory();
        cf.setResourceURL(scriptFileUrl);
        if (externalProperties != null) {
            cf.setExternalParameters(externalProperties);
        }
        return new EtlExecutor(cf.createConfiguration());
    }

    //Runnable/Callable convenience interfaces

    /**
     * A runnable adapter for {@link #execute()} method.
     * <p>Please note that due to a checked
     * exceptions limitation a {@link scriptella.core.SystemException} is thrown instead of
     * the {@link scriptella.execution.EtlExecutorException}.
     *
     * @throws SystemException a wrapped {@link scriptella.execution.EtlExecutorException}.
     * @see #execute()
     */
    public void run() throws SystemException {
        try {
            execute();
        } catch (EtlExecutorException e) {
            throw new SystemException(e.getMessage(), e);
        }
    }

    /**
     * A synonym for {@link #execute()}.
     */
    public ExecutionStatistics call() throws EtlExecutorException {
        return execute();
    }
}
