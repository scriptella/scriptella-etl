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
package scriptella.tools.launcher;

import scriptella.configuration.ConfigurationEl;
import scriptella.configuration.ConfigurationFactory;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;
import scriptella.execution.ExecutionStatistics;
import scriptella.interactive.ConsoleProgressIndicator;
import scriptella.interactive.LoggingConfigurer;
import scriptella.interactive.ProgressIndicator;
import scriptella.tools.template.TemplateManager;
import scriptella.util.BugReport;
import scriptella.util.CollectionUtils;
import scriptella.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
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
    private static final PrintStream out = System.out;
    private static final PrintStream err = System.err;

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
    private Map<String,String> properties;
    public static final String DEFAULT_FILE_NAME = "etl.xml";

    public EtlLauncher() {
    }

    /**
     * Launches ETL script using command line arguments.
     * @param args command line arguments.
     * @return exit error code.
     * @see System#exit(int)
     */
    int launch(String[] args) {
        ConsoleHandler h = new ConsoleHandler();

        h.setFormatter(STD_FORMATTER);
        h.setLevel(Level.INFO);
        boolean failed = false;
        List<File> files = new ArrayList<File>();
        ConsoleProgressIndicator indicator = new ConsoleProgressIndicator("Execution Progress");

        try {
            for (String arg : args) {
                if (arg.startsWith("-h")) {
                    printUsage();
                    return 0;
                }
                if (arg.startsWith("-d")) {
                    h.setLevel(Level.FINE);
                    continue;
                }
                if (arg.startsWith("-q")) {
                    h.setLevel(Level.WARNING);
                    continue;
                }
                if (arg.startsWith("-v")) {
                    printVersion();
                    return 0;
                }
                if (arg.startsWith("-t")) {
                    template();
                    return 0;
                }
                if (arg.startsWith("-")) {
                    err.println("Unrecognized option "+arg);
                    return 3;
                }
                if (!arg.startsWith("-")) {
                    files.add(resolveFile(null, arg));
                }
            }


            if (files.isEmpty()) { //adding default name if no files specified
                files.add(resolveFile(null, null));
            }
        } catch (FileNotFoundException e) {
            err.println(e.getMessage());
            return 2;
        }

        if (indicator != null) {
            setProgressIndicator(indicator);
        }

        LoggingConfigurer.configure(h);
        setProperties(CollectionUtils.asMap(System.getProperties()));
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
                    err.println("---------------Debug Stack Trace-----------------");
                    Throwable t = e.getCause() == null ? e : e.getCause();
                    t.printStackTrace();
                }
            }
        }
        LoggingConfigurer.remove(h);

        return failed?1:0;
    }

    protected void printVersion() {
        Package p = getClass().getPackage();
        if (p != null && p.getSpecificationTitle() != null && p.getImplementationVersion() != null) {
            out.println(p.getSpecificationTitle() + " Version " + p.getImplementationVersion());
        } else {
            out.println("Scriptella version information unavailable");
        }
    }

    protected void printUsage() {
        out.println("scriptella [-options] [<file 1> ... <file N>]");
        out.println("Options:");
        out.println("  -help,     -h        display help ");
        out.println("  -debug,    -d        print debugging information");
        out.println("  -quiet,    -q        be extra quiet");
        out.println("  -version,  -v        print version");
        out.println("  -template, -t        creates an etl.xml template file in the current directory");
    }

    protected void template() {
        try {
            new TemplateManager().create();
        } catch (Exception e) {
            err.println(e.getMessage());
        }
    }

    public void setProperties(final Map<String,String> props) {
        properties=props;
    }

    public void setProgressIndicator(final ProgressIndicator indicator) {
        this.indicator = indicator;
    }

    public void execute(final File file)
            throws EtlExecutorException {
        try {
            factory.setResourceURL(file.toURL());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Wrong file path " +
                    file.getPath(), e);
        }

        factory.setExternalProperties(properties);
        final ConfigurationEl c = factory.createConfiguration();

        exec.setConfiguration(c);
        ExecutionStatistics st = exec.execute(indicator);

        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("ETL file " + file + " has been successfully executed. Execution statistics:\n" + st.toString());
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
            if (!isFile(f) && name.indexOf('.') < 0) { //not a file and no extension
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
        EtlLauncher launcher=new EtlLauncher();
        System.exit(launcher.launch(args));
    }

}
