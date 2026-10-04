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

import scriptella.configuration.ConfigurationEl;
import scriptella.configuration.ConfigurationFactory;
import scriptella.configuration.SqlFileConfigurationFactory;
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
 * Executes a Scriptella ETL configuration loaded from XML or created in memory.
 *
 * <p>The simplest way to run an ETL XML file is:</p>
 * <pre>{@code
 * EtlExecutor executor = EtlExecutor.newExecutor(new File("etl.xml"));
 * ExecutionStatistics statistics = executor.execute();
 * }</pre>
 *
 * <p>{@link #newExecutor(File)} and {@link #newExecutor(URL)} use the current
 * {@linkplain System#getProperties() system properties} as external ETL
 * parameters. To supply parameters explicitly, use
 * {@link #newExecutor(URL, Map)}. External parameters take precedence over
 * properties declared in the ETL file.</p>
 *
 * <h3>Direct SQL file execution</h3>
 * <p>For a single SQL file, use {@link #newSqlFileExecutor(File, String, String, String)}
 * to create a normal executor with one JDBC connection and one script in memory.
 * No ETL XML file is needed:</p>
 * <pre>{@code
 * EtlExecutor executor = EtlExecutor.newSqlFileExecutor(
 *     new File("schema.sql"), "jdbc:postgresql://localhost/app", "app", "secret");
 * ExecutionStatistics statistics = executor.execute();
 * long updatedRows = statistics.getUpdateCount();
 * }</pre>
 * <p>SQL files are read as UTF-8 at execution time. JDBC driver JARs must be on
 * the classpath. As with the default XML factories, SQL variables come from
 * JVM system properties, captured when the factory is called.
 * Scriptella's normal SQL substitution is enabled by default. Set
 * {@code substitution} to {@code false} to preserve dollar and question-mark
 * expressions literally. Use {@code ?name} bindings for data values; text
 * substitutions such as {@code ${name}} do not escape SQL.</p>
 * <p>The returned executor uses the same transactions, cleanup, progress,
 * statistics, optional JMX, and cancellation as an XML-based executor.
 * Execution commits on success and attempts rollback on failure; database DDL
 * may not support rollback. SQL files support updates and DDL. Use an ETL query
 * element when results need to be processed.</p>
 *
 * <p>For more control, use {@link ConfigurationFactory} to parse the ETL file,
 * then pass the resulting {@link ConfigurationEl} to
 * {@link #EtlExecutor(ConfigurationEl)}.</p>
 *
 * <h3>ETL Cancellation</h3>
 * <p>Scriptella uses the standard Java {@link Thread#interrupt()} mechanism.
 * Interrupt the thread that is running {@link #execute()}; the engine then
 * attempts to roll back changes made during the ETL operation. A cancellation
 * is reported as an {@link EtlExecutorException} for which
 * {@link EtlExecutorException#isCancelled()} returns {@code true}.</p>
 *
 * <p>{@link java.util.concurrent.ExecutorService} and
 * {@link java.util.concurrent.Future} can also be used to submit and cancel an
 * execution.</p>
 *
 * <h3>Integration with third-party systems</h3>
 * <p>For convenience, {@code EtlExecutor} implements {@link Runnable} and
 * {@link Callable}. Use {@link #call()} when execution statistics or the checked
 * {@link EtlExecutorException} are needed. {@link #run()} discards the statistics
 * and wraps execution failures in a {@link SystemException}.</p>
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
     * Creates an ETL executor without a configuration.
     * Call {@link #setConfiguration(ConfigurationEl)} before executing it.
     */
    public EtlExecutor() {
    }

    /**
     * Creates an ETL executor for a parsed or programmatically constructed configuration.
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
     * @param indicator progress indicator to use, or {@code null} if progress
     *                  reporting is not required.
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
            if (!suppressStatistics) {
                ctx.getStatisticsBuilder().getStatistics().updateCount = ctx.session.getUpdateCount();
            }
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
     * Creates an ETL executor for the specified script URL.
     * <p>Calls {@link #newExecutor(java.net.URL, java.util.Map)} and passes {@link System#getProperties() System properties}
     * as external properties.
     *
     * @param scriptFileUrl URL of the ETL file.
     * @return configured instance of script executor.
     */
    @ThreadSafe
    public static EtlExecutor newExecutor(final URL scriptFileUrl) {
        return newExecutor(scriptFileUrl, CollectionUtils.asMap(System.getProperties()));
    }

    /**
     * Creates an ETL executor for the specified script URL and external
     * parameters.
     *
     * @param scriptFileUrl      URL of the ETL file.
     * @param externalProperties external ETL parameters, or {@code null} for
     *                           none; these take precedence over properties in
     *                           the ETL file.
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

    /**
     * Creates a SQL file executor without explicit JDBC credentials.
     * @param file UTF-8 SQL file
     * @param url JDBC connection URL
     * @return a normal ETL executor
     * @see #newSqlFileExecutor(File, String, String, String)
     */
    public static EtlExecutor newSqlFileExecutor(File file, String url) {
        return newSqlFileExecutor(file, url, null, null);
    }

    /**
     * Creates an executor for a UTF-8 SQL file and a JDBC URL, without ETL XML.
     * System properties are available for SQL substitution. No connection is
     * opened until execution. JDBC driver JARs must be on the classpath.
     * @param file UTF-8 SQL file
     * @param url JDBC connection URL
     * @param user optional username
     * @param password optional password
     * @return a normal ETL executor
     * @see #newSqlFileExecutor(File, String, String, String, String, boolean)
     */
    public static EtlExecutor newSqlFileExecutor(File file, String url, String user, String password) {
        return newSqlFileExecutor(file, url, user, password, null, true);
    }

    /**
     * Creates a SQL file executor using the normal ETL execution lifecycle.
     * Execution commits on success and attempts rollback on failure; DDL rollback
     * depends on the database. SELECT results are unsupported; use an ETL query.
     * SQL variables come from a snapshot of JVM system properties. Text substitution
     * does not escape SQL; use ?name bindings for data values.
     *
     * @param file UTF-8 SQL file, read at execution time
     * @param url JDBC connection URL
     * @param user optional username
     * @param password optional password
     * @param driver optional JDBC driver class; null uses JDBC automatic registration
     * @param substitution whether Scriptella SQL substitution is enabled
     * @return a normal ETL executor supporting execute(), progress, JMX, and cancellation
     * @throws IllegalArgumentException if file is null or the URL does not start with jdbc:
     */
    public static EtlExecutor newSqlFileExecutor(final File file, String url, String user, String password,
                                                String driver, boolean substitution) {
        return new EtlExecutor(SqlFileConfigurationFactory.create(file, url, user, password, driver, substitution));
    }

    //Runnable/Callable convenience interfaces

    /**
     * A {@link Runnable} adapter for {@link #execute()}.
     * <p>Because {@link Runnable#run()} cannot declare checked exceptions, this
     * method wraps an {@link EtlExecutorException} in a {@link SystemException}.</p>
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
     * A {@link Callable} adapter for {@link #execute()}.
     *
     * @return execution statistics for the ETL execution.
     * @throws EtlExecutorException if ETL execution fails.
     */
    public ExecutionStatistics call() throws EtlExecutorException {
        return execute();
    }
}
