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
package scriptella.tools.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.FileSet;
import scriptella.interactive.ConsoleProgressIndicator;
import scriptella.tools.launcher.EtlLauncher;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;


/**
 * Ant task for ETL scripts execution.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlExecuteTask extends EtlTaskBase {
    private List<FileSet> filesets = new ArrayList<FileSet>();
    private final EtlLauncher launcher = new EtlLauncher();
    private String maxmemory;
    private boolean fork;
    private boolean nostat;

    public boolean isFork() {
        return fork;
    }

    public void setFork(final boolean fork) {
        this.fork = fork;
    }

    public boolean isNostat() {
        return nostat;
    }

    public void setNostat(boolean nostat) {
        this.nostat = nostat;
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
        launcher.setProperties(getProperties());
        launcher.setNoStat(nostat);

        setupLogging();
        for (File file : files) {
            try {
                launcher.execute(file);
            } catch (Exception e) {
                throw new BuildException("Unable to execute file " + file +
                        ": " + e.getMessage(), e);
            }
        }
        resetLogging();
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

        if (nostat) {
            line.append("-nostat ");
        }

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
