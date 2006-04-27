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
import scriptella.sql.SQLEngine;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptsRunner {
    private static final Logger LOG = Logger.getLogger(ScriptsRunner.class.getName());

    static {
        Logging.configure();
    }

    private ScriptsExecutor exec = new ScriptsExecutor();
    private ConfigurationFactory factory = new ConfigurationFactory();
    private ProgressIndicator indicator = null;

    private static void printUsage() {
        System.out.println("Usage java " + ScriptsRunner.class.getName() +
                " [-options] [<file 1> ... <file N>]");
        System.out.println("where options include:");
        System.out.println("   -h displays help ");
    }

    public void setDriversClassLoader(final ClassLoader classLoader) {
        SQLEngine.setDriversClassLoader(classLoader);
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
                    file.getPath(),e);
        }

        final ConfigurationEl c = factory.createConfiguration();
        exec.setConfiguration(c);

        return exec.execute(indicator);
    }

    public static void main(final String args[]) {
        boolean failed = false;
        List<File> files = new ArrayList<File>();
        ConsoleProgressIndicator indicator = new ConsoleProgressIndicator("Scripts execution");
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-h")) {
                printUsage();
                return;
            }
            files.add(new File(args[i]));
        }
        if (files.isEmpty()) { //adding default name if no files specified
            files.add(new File("script.xml"));
        }
        ScriptsRunner runner = new ScriptsRunner();

        if (indicator!=null) {
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
            }
        }

        if (failed) {
            System.exit(1);
        }
    }
}
