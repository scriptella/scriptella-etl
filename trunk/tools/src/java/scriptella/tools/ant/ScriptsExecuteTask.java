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
package scriptella.tools.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.FileSet;
import scriptella.interactive.ConsoleProgressIndicator;
import scriptella.tools.ScriptsRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Ant task for scripts execution.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptsExecuteTask extends ClasspathSupportTask {
    private List<FileSet> filesets = new ArrayList<FileSet>();
    private String maxmemory;
    private boolean fork;
    private boolean inheritAll;

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

    public void addFileset(final FileSet set) {
        filesets.add(set);
    }

    public void setFile(final File file) {
        FileSet f = new FileSet();
        f.setFile(file);
        filesets.add(f);
    }

    public void execute() throws BuildException {
        List<File> files = new ArrayList<File>();

        if (filesets.isEmpty()) { //if no files - script.xml is a default name
            files.add(new File(getProject().getBaseDir(), "script.xml"));
        } else {
            for (FileSet fs : filesets) {
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                File srcDir = fs.getDir(getProject());

                String srcFiles[] = ds.getIncludedFiles();

                for (String fName : srcFiles) {
                    final File file = new File(srcDir, fName);
                    files.add(file);
                }
            }
        }

        if (fork) {
            fork(files);
        } else {
            execute(files);
        }
    }

    private void execute(final List<File> files) {
        ScriptsRunner runner = null;
        runner = new ScriptsRunner();
        runner.setDriversClassLoader(getClassLoader());
        runner.setProgressIndicator(new ConsoleProgressIndicator() {
            protected void println(final Object o) {
                if (o != null) {
                    log(o.toString(), Project.MSG_VERBOSE);
                }
            }
        });

        if (inheritAll) { //inherit ant properties - not supported in forked mode yet
            runner.setProperties(getProject().getProperties());
        }

        for (File file : files) {
            try {
                runner.execute(file);
                log("Script " + file.getPath() +
                        " has been executed successfully");
            } catch (Exception e) {
                throw new BuildException("Unable to execute file " + file +
                        ": " + e.getMessage(), e);
            }
        }
    }

    private void fork(final List<File> files) {
        Java j = new Java();
        j.setFork(true);
        j.setProject(getProject());
        j.setClasspath(getClasspath());
        j.setClassname(ScriptsRunner.class.getName());

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
}
