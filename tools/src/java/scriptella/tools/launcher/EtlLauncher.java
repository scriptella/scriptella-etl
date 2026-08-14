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
package scriptella.tools.launcher;

import scriptella.configuration.ConfigurationEl;
import scriptella.configuration.ConfigurationFactory;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;
import scriptella.execution.ExecutionStatistics;
import scriptella.execution.JmxEtlManager;
import scriptella.interactive.ConsoleProgressIndicator;
import scriptella.interactive.LoggingConfigurer;
import scriptella.interactive.ProgressIndicator;
import scriptella.spi.AbstractScriptellaDriver;
import scriptella.tools.template.TemplateManager;
import scriptella.util.CollectionUtils;
import scriptella.util.IOUtils;
import scriptella.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


/**
 * Command line launcher.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlLauncher {
    private static final Logger LOG = Logger.getLogger(EtlLauncher.class.getName());

    /**
     * Error codes returned by the launcher.
     */
    public enum ErrorCode {
        OK(0), FAILED(1), FILE_NOT_FOUND(2), UNRECOGNIZED_OPTION(3);

        ErrorCode(int code) {
            errorCode = code;
        }

        private int errorCode;

        public int getErrorCode() {
            return errorCode;
        }
    }

    public static final Formatter STD_FORMATTER = new Formatter() {
        private final MessageFormat f = new MessageFormat("{0,date} {0,time} <{1}> {2}");
        private final Object args[] = new Object[3]; //arguments for formatter
        private final StringBuffer sb = new StringBuffer();
        private final Date d = new Date();

        public synchronized String format(final LogRecord record) {
            d.setTime(record.getMillis());
            args[0] = d;
            args[1] = record.getLevel().getLocalizedName();
            args[2] = record.getMessage();

            f.format(args, sb, null);
            final Throwable err = record.getThrown();
            sb.append('\n');
            if (err != null) {
                sb.append(err.getMessage());
                sb.append('\n');
            }
            final String s = sb.toString();
            sb.setLength(0);
            return s;
        }
    };

    private EtlExecutor exec = new EtlExecutor();
    private ConfigurationFactory factory = new ConfigurationFactory();
    private ProgressIndicator indicator;
    private Map<String, ?> properties;
    public static final String DEFAULT_FILE_NAME = "etl.xml";

    public EtlLauncher() {
        exec.setJmxEnabled(true); //JMX monitoring is always enabled when launcher is used
    }

    /**
     * Stream used for launcher messages. Overridable in tests.
     */
    protected PrintStream getOut() {
        return System.out;
    }

    /**
     * Stream used for launcher errors. Overridable in tests.
     */
    protected PrintStream getErr() {
        return System.err;
    }

    /**
     *
     * @param suppressStatistics true if statistics must be suppressed.
     * @see scriptella.execution.EtlExecutor#setSuppressStatistics(boolean)
     */
    public void setNoStat(boolean suppressStatistics) {
        exec.setSuppressStatistics(suppressStatistics);
    }

    /**
     * Suppress JMX MBean registration for monitoring.
     *
     * @param noJmx true if JMX should be suppressed.
     */
    public void setNoJmx(boolean noJmx) {
        exec.setJmxEnabled(!noJmx);
    }

    /**
     * Launches ETL script using command line arguments.
     *
     * @param args command line arguments.
     * @return exit error code.
     * @see System#exit(int)
     */
    ErrorCode launch(String[] args) {
        ConsoleHandler h = new ConsoleHandler();

        h.setFormatter(STD_FORMATTER);
        h.setLevel(Level.INFO);
        boolean failed = false;
        List<File> files = new ArrayList<File>();
        ConsoleProgressIndicator indicator = new ConsoleProgressIndicator("Execution Progress");
        boolean defaultFile = false;

        try {
            List<String> arguments = new ArrayList<String>(Arrays.asList(args));
            while (!arguments.isEmpty()) {
                String arg = arguments.get(0);
                arguments.remove(0);
                if (isHelpOption(arg)) {
                    printUsage();
                    return ErrorCode.OK;
                }
                if (isDebugOption(arg)) {
                    h.setLevel(Level.FINE);
                    continue;
                }
                if (isQuietOption(arg)) {
                    h.setLevel(Level.WARNING);
                    continue;
                }
                if (isVersionOption(arg)) {
                    printVersion();
                    return ErrorCode.OK;
                }
                if (isTemplateOption(arg)) {
                    return template(arguments);
                }
                if (isNoStatOption(arg)) {
                    setNoStat(true);
                    continue;
                }
                if (isNoJmxOption(arg)) {
                    setNoJmx(true);
                    continue;
                }
                if (arg.startsWith("-")) {
                    getErr().println("Unrecognized option " + arg);
                    return ErrorCode.UNRECOGNIZED_OPTION;
                }
                files.add(resolveFile(null, arg));
            }

            if (files.isEmpty()) { //adding default name if no files specified
                defaultFile = true;
                files.add(resolveFile(null, null));
            }
        } catch (FileNotFoundException e) {
            if (defaultFile) {
                getErr().println("ETL file " + DEFAULT_FILE_NAME + " was not found.");
                getErr().println();
                getErr().println("Run with --help for usage.");
            } else {
                getErr().println(e.getMessage());
            }
            return ErrorCode.FILE_NOT_FOUND;
        }

        if (indicator != null) {
            setProgressIndicator(indicator);
        }

        LoggingConfigurer.configure(h);
        if (properties == null) {
            setProperties(CollectionUtils.asMap(System.getProperties()));
        }
        for (File file : files) {
            try {
                execute(file);
            } catch (Exception e) {
                failed = true;
                LOG.log(Level.SEVERE,
                        "Script " + file + " execution failed.", e);
                if (BugReport.isPossibleBug(e)) {
                    LOG.log(Level.SEVERE, new BugReport(e).toString());
                } else if (h.getLevel().intValue() < Level.INFO.intValue()) {
                    //Print stack trace of exception in debug mode
                    getErr().println("---------------Debug Stack Trace-----------------");
                    Throwable t = e.getCause() == null ? e : e.getCause();
                    t.printStackTrace(getErr());
                }
            }
        }
        LoggingConfigurer.remove(h);

        return failed ? ErrorCode.FAILED : ErrorCode.OK;
    }

    private static boolean isHelpOption(String arg) {
        return "-h".equals(arg) || "--help".equals(arg) || "-help".equals(arg);
    }

    private static boolean isDebugOption(String arg) {
        return "-d".equals(arg) || "--debug".equals(arg) || "-debug".equals(arg);
    }

    private static boolean isQuietOption(String arg) {
        return "-q".equals(arg) || "--quiet".equals(arg) || "-quiet".equals(arg);
    }

    private static boolean isVersionOption(String arg) {
        return "-v".equals(arg) || "--version".equals(arg) || "-version".equals(arg);
    }

    private static boolean isTemplateOption(String arg) {
        return "-t".equals(arg) || "--template".equals(arg) || "-template".equals(arg);
    }

    private static boolean isNoStatOption(String arg) {
        return "--no-stat".equals(arg) || "-nostat".equals(arg);
    }

    private static boolean isNoJmxOption(String arg) {
        return "--no-jmx".equals(arg) || "-nojmx".equals(arg);
    }

    protected void printVersion() {
        String v = AbstractScriptellaDriver.getScriptellaVersion();
        String p = AbstractScriptellaDriver.getScriptellaTitle();
        if (p != null && v != null) {
            getOut().println(p + " Version " + v);
        } else {
            getOut().println("Scriptella version information unavailable");
        }
    }

    protected void printUsage() {
        PrintStream out = getOut();
        out.println("Scriptella ETL");
        out.println();
        out.println("A lightweight tool for moving and transforming data between databases,");
        out.println("files, and other systems using SQL and other scripting languages.");
        out.println();
        out.println("Usage:");
        out.println("  java -jar scriptella.jar [options] [<etl-file>...]");
        out.println();
        out.println("If no ETL file is specified, Scriptella runs etl.xml from the current directory.");
        out.println();
        out.println("Examples:");
        out.println("  java -jar scriptella.jar load.etl.xml");
        out.println("  java -jar scriptella.jar -q --no-jmx load.etl.xml");
        out.println("  java -Dinput.file=data.csv -jar scriptella.jar load.etl.xml");
        out.println();
        out.println("Options:");
        out.println("  -h, --help             Show this help");
        out.println("  -v, --version          Show version information");
        out.println("  -d, --debug            Enable debug logging");
        out.println("  -q, --quiet            Suppress informational output");
        out.println("      --no-stat          Disable execution statistics");
        out.println("      --no-jmx           Disable JMX registration");
        out.println("  -t, --template [name]  Create an ETL template in the current directory");
        out.println();
        out.println("Documentation:");
        out.println("  https://scriptella.org/");
    }

    protected ErrorCode template(List<String> args) {
        try {
            String name = args.isEmpty() ? null : args.get(0);
            String props = null;
            if (name != null) { //if not an option - shift the argument
                args.remove(0);
                props = args.isEmpty() ? null : args.get(0);
            }
            TemplateManager.create(name, props);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Template generation failed", e);
            return ErrorCode.FAILED;
        }
        return ErrorCode.OK;
    }

    /**
     * Sets additional properties available for ETL.
     * <p>By default {@link System#getProperties()} is used.
     *
     * @param props properties map.
     */
    public void setProperties(final Map<String, ?> props) {
        properties = props;
    }

    public void setProgressIndicator(final ProgressIndicator indicator) {
        this.indicator = indicator;
    }

    public void execute(final File file)
            throws EtlExecutorException {
        try {
            factory.setResourceURL(IOUtils.toUrl(file));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Wrong file path " +
                    file.getPath(), e);
        }

        factory.setExternalParameters(properties);
        final ConfigurationEl c = factory.createConfiguration();

        exec.setConfiguration(c);
        ExecutionStatistics st = exec.execute(indicator);
        if (LOG.isLoggable(Level.INFO)) {
            if (!exec.isSuppressStatistics()) {
                LOG.info("Execution statistics:\n" + st.toString());
            }
            LOG.info("Successfully executed ETL file " + file);
        }
    }

    /**
     * Resolves ETL file using the following rule:
     * if specified file exists - it is returned, otherwise if file has no extension
     * <code>name</code>+&quot;.etl.xml&quot; file is checked for presence and returned.
     *
     * @param dir  parent directory, may be null.
     * @param name file name, may be null in this case default name is used.
     * @return resolved ETL file.
     * @throws FileNotFoundException if ETL file cannot be found.
     */
    public File resolveFile(File dir, String name) throws FileNotFoundException {
        File f;
        if (StringUtils.isEmpty(name)) {
            f = new File(dir, DEFAULT_FILE_NAME);
        } else {
            f = new File(dir, name);
            if (!isFile(f) && f.getName().indexOf('.') < 0) { //not a file and no extension
                f = new File(dir, name + '.' + DEFAULT_FILE_NAME);
            }
        }
        if (!isFile(f)) {
            throw new FileNotFoundException("ETL file " + f + " was not found.");
        }

        return f.getAbsoluteFile();
    }

    /**
     * Overridable for testing.
     */
    protected boolean isFile(File file) {
        return file.isFile();
    }

    public static void main(final String args[]) {
        EtlLauncher launcher = new EtlLauncher();
        System.exit(launcher.launch(args).getErrorCode());
    }

    /**
     * Shutdown hook for ETL cancellation.
     */
    private static class EtlShutdownHook extends Thread {
        private static final EtlShutdownHook INSTANCE = new EtlShutdownHook();

        private EtlShutdownHook() {
            setName("ETL Cancellation Thread");
        }

        public void run() {
            //if any mbean present - inform user about cancellation
            if (!JmxEtlManager.findEtlMBeans().isEmpty()) {
                System.out.println("Cancelling ETL tasks and rolling back changes...");
            }
            //Cancel all ETL task, the findEtlMBeans result may be stale 
            int i = JmxEtlManager.cancelAll();
            if (i > 1) {
                System.out.println(i + " ETL tasks cancelled");
            } else if (i == 1) {
                System.out.println("ETL cancelled");
            }
        }
    }

    static {
        //Register a system shutdown hook which cancels all
        //in-progress ETL tasks on VM exit.
        try {
            Runtime.getRuntime().addShutdownHook(EtlShutdownHook.INSTANCE);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to add shutdown hook. ETL will not be rolled back on abnormal termination.", e);
        }
    }


}
