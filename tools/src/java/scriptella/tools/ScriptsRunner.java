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
package scriptella.tools;

import scriptella.configuration.ConfigurationEl;
import scriptella.configuration.ConfigurationFactory;
import scriptella.execution.ExecutionStatistics;
import scriptella.execution.ScriptsExecutor;
import scriptella.execution.ScriptsExecutorException;
import scriptella.interactive.ConsoleProgressIndicator;
import scriptella.interactive.ProgressIndicator;
import scriptella.util.BugReport;

import java.io.File;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptsRunner {
    private static final Logger LOG = Logger.getLogger(ScriptsRunner.class.getName());

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

    private ScriptsExecutor exec = new ScriptsExecutor();
    private ConfigurationFactory factory = new ConfigurationFactory();
    private ProgressIndicator indicator;

    private static void printUsage() {
        System.out.println("scriptella [-options] [<file 1> ... <file N>]");
        System.out.println("Options:");
        System.out.println("  -help, -h           displays help ");
        System.out.println("  -debug, -d          print debugging information");
        System.out.println("  -quiet, -q          be extra quiet");
    }

    public void setProperties(final Map<?, ?> props) {
        exec.setExternalProperties(props);
    }

    public void setProgressIndicator(final ProgressIndicator indicator) {
        this.indicator = indicator;
    }

    public ExecutionStatistics execute(final File file)
            throws ScriptsExecutorException {
        try {
            factory.setResourceURL(file.toURL());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Wrong file path " +
                    file.getPath(), e);
        }

        final ConfigurationEl c = factory.createConfiguration();
        exec.setConfiguration(c);
        return exec.execute(indicator);
    }

    public static void main(final String args[]) {
        Handler h = new ConsoleHandler();
        h.setFormatter(STD_FORMATTER);
        h.setLevel(Level.INFO);
        boolean failed = false;
        List<File> files = new ArrayList<File>();
        ConsoleProgressIndicator indicator = new ConsoleProgressIndicator("Scripts execution");
        for (String arg : args) {
            if (arg.startsWith("-h")) {
                printUsage();
                return;
            }
            if (arg.startsWith("-d")) {
                h.setLevel(Level.FINE);
                continue;
            }
            if (arg.startsWith("-q")) {
                h.setLevel(Level.WARNING);
                continue;
            }

            files.add(new File(arg));
        }

        Logging.configure(h);

        if (files.isEmpty()) { //adding default name if no files specified
            files.add(new File("script.xml"));
        }
        ScriptsRunner runner = new ScriptsRunner();

        if (indicator != null) {
            runner.setProgressIndicator(indicator);
        }

        for (File file : files) {
            try {
                final ExecutionStatistics st = runner.execute(file);
                LOG.info("Script " + file +
                        " has been executed successfully");
                LOG.info(st.toString());
            } catch (Exception e) {
                failed = true;
                LOG.log(Level.SEVERE,
                        "Script " + file + " execution failed.", e);
                if (BugReport.isPossibleBug(e)) {
                    LOG.log(Level.SEVERE, new BugReport(e).toString());
                }
            }
        }

        if (failed) {
            System.exit(1);
        }
    }
}
