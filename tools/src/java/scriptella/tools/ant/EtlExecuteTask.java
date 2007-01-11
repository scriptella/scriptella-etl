/*
 * Copyright 2006-2007 The Scriptella Project Team.
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
package scriptella.tools.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.FileSet;
import scriptella.interactive.ConsoleProgressIndicator;
import scriptella.interactive.LoggingConfigurer;
import scriptella.tools.launcher.EtlLauncher;
import scriptella.util.CollectionUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;


/**
 * Ant task for STL scripts execution.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlExecuteTask extends Task {
    private List<FileSet> filesets = new ArrayList<FileSet>();
    private final EtlLauncher launcher = new EtlLauncher();
    private String maxmemory;
    private boolean fork;
    private boolean inheritAll=true;
    private boolean debug;
    private boolean quiet;

    public boolean isFork() {
        return fork;
    }

    public void setFork(final boolean fork) {
        this.fork = fork;
    }

    /**
     * @return Max amount of memory to allocate to the forked VM (ignored if fork is disabled)
     */
    public String getMaxmemory() {
        return maxmemory;
    }

    public void setMaxmemory(final String maxmemory) {
        this.maxmemory = maxmemory;
    }

    /**
     * @return true if pass all ant properties to script executor. Default value is false.
     */
    public boolean isInheritAll() {
        return inheritAll;
    }

    /**
     * @param inheritAll true if pass all ant properties to script executor.
     */
    public void setInheritAll(final boolean inheritAll) {
        this.inheritAll = inheritAll;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public void addFileset(final FileSet set) {
        filesets.add(set);
    }

    public void setFile(final String fileName) throws FileNotFoundException {
        FileSet f = new FileSet();
        f.setFile(launcher.resolveFile(null, fileName));
        filesets.add(f);
    }

    public void execute() throws BuildException {
        List<File> files = new ArrayList<File>();

        try {
            if (filesets.isEmpty()) { //if no files - use file default name
                files.add(launcher.resolveFile(getProject().getBaseDir(), null));
            } else {
                for (FileSet fs : filesets) {
                    DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                    File srcDir = fs.getDir(getProject());

                    String srcFiles[] = ds.getIncludedFiles();

                    for (String fName : srcFiles) {
                        File file = launcher.resolveFile(srcDir, fName);
                        files.add(file);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new BuildException(e.getMessage(), e);
        }

        if (fork) {
            fork(files);
        } else {
            execute(files);
        }
    }

    private void execute(final List<File> files) {
        launcher.setProgressIndicator(new ConsoleProgressIndicator());

        if (inheritAll) { //inherit ant properties - not supported in forked mode yet
            launcher.setProperties(getProject().getProperties());
        } else {
            launcher.setProperties(CollectionUtils.asMap(System.getProperties()));

        }

        Handler h = new AntHandler();
        h.setLevel(Level.INFO);
        if (debug) {
            h.setLevel(Level.FINE);
        }
        if (quiet) {
            h.setLevel(Level.WARNING);
        }
        LoggingConfigurer.configure(h);
        for (File file : files) {
            try {
                launcher.execute(file);
            } catch (Exception e) {
                throw new BuildException("Unable to execute file " + file +
                        ": " + e.getMessage(), e);
            }
        }
        LoggingConfigurer.remove(h);
    }

    /**
     * TODO Implement fork correctly - use ant LoaderUtils to get scriptella.jar location
     * TODO Output errors
     */
    private void fork(final List<File> files) {
        Java j = new Java();
        j.setFork(true);
        j.setProject(getProject());
//        j.setClasspath(getClasspath());

        j.setClassname(EtlLauncher.class.getName());

        StringBuilder line = new StringBuilder();

        for (File file : files) {
            line.append(file.getPath()).append(' ');
        }
        j.createArg().setLine(line.toString());

        if (maxmemory != null) {
            j.setMaxmemory(maxmemory);
        }

        int r = j.executeJava();
        if (r != 0) {
            throw new BuildException("Unable to execute files: " + files +
                    ". See error log.");
        }
    }

    class AntHandler extends Handler {
        private StringBuilder sb = new StringBuilder();

        public synchronized void publish(LogRecord record) {
            String msg = record.getMessage();
            if (msg != null) {
                sb.append(msg);
            }
            Throwable thrown = record.getThrown();
            if (thrown != null) {
                sb.append("\n").append(thrown.toString()).append('\n');
            }
            final int lev = convert(record.getLevel());
            if (lev >= 0) {
                log(sb.toString(), lev);
            }
            sb.setLength(0);
        }

        /**
         * Converts JUL level to appropriate Ant message priority
         *
         * @param level JUL level
         * @return Ant message priority
         */
        private int convert(Level level) {
            final int lev = level.intValue();
            if (lev >= Level.SEVERE.intValue()) {
                return Project.MSG_ERR;
            }
            if (lev >= Level.WARNING.intValue()) {
                return Project.MSG_WARN;
            }
            if (lev >= Level.INFO.intValue()) {
                return Project.MSG_INFO;
            }
            if (debug) {
                return Project.MSG_INFO;
            }
            if (lev >= Level.FINE.intValue()) {
                return Project.MSG_VERBOSE;
            }
            if (lev > Level.OFF.intValue()) {
                return Project.MSG_DEBUG;
            }
            return -1;
        }

        public void flush() {
        }

        public void close() {
        }
    }

}
