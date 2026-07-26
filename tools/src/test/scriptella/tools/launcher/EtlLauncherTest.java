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

import scriptella.DBTestCase;
import scriptella.core.SystemException;
import scriptella.execution.EtlExecutorException;
import scriptella.tools.template.TemplateManagerTest;
import scriptella.util.IOUtils;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Tests for {@link EtlLauncher}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlLauncherTest extends DBTestCase {

    public void testLaunch() {
        final List<String> files = new ArrayList<String>();

        EtlLauncher etlLauncher = new EtlLauncher() {
            @Override
            protected boolean isFile(File file) {
                return file.getName().indexOf("_nofile_") < 0;
            }

            @Override
            public void execute(final File file) {
                files.add(file.getName());
            }
        };
        assertEquals(EtlLauncher.ErrorCode.OK, etlLauncher.launch(new String[]{"-v"}));
        assertEquals(EtlLauncher.ErrorCode.UNRECOGNIZED_OPTION, etlLauncher.launch(new String[]{"-nosuchproperty"}));
        assertEquals(EtlLauncher.ErrorCode.OK, etlLauncher.launch(new String[]{"-h"}));
        assertEquals(EtlLauncher.ErrorCode.OK, etlLauncher.launch(new String[]{}));
        assertEquals(1, files.size());
        assertEquals("etl.xml", files.get(0));
        files.clear();
        assertEquals(EtlLauncher.ErrorCode.FILE_NOT_FOUND, etlLauncher.launch(new String[]{"_nofile_", "etl.xml"}));
        assertEquals(0, files.size());
    }

    public void testNoEtlFile() {
        CapturingLauncher etlLauncher = new CapturingLauncher() {
            @Override
            protected boolean isFile(File file) {
                return false;
            }
        };
        assertEquals(EtlLauncher.ErrorCode.FILE_NOT_FOUND, etlLauncher.launch(new String[]{}));
        String err = etlLauncher.errText();
        assertTrue(err.contains("ETL file etl.xml was not found."));
        assertTrue(err.contains("Run with --help for usage."));
    }

    public void testExplicitMissingFileHasNoDefaultHint() {
        CapturingLauncher etlLauncher = new CapturingLauncher() {
            @Override
            protected boolean isFile(File file) {
                return false;
            }

            @Override
            public void execute(final File file) {
                fail("ETL must not execute when file is missing");
            }
        };
        assertEquals(EtlLauncher.ErrorCode.FILE_NOT_FOUND,
                etlLauncher.launch(new String[]{"missing.etl.xml"}));
        String err = etlLauncher.errText();
        assertTrue(err.contains("ETL file"));
        assertTrue(err.contains("was not found"));
        assertFalse(err.contains("Run with --help for usage."));
    }

    public void testHelpOptions() {
        CapturingLauncher launcher = new CapturingLauncher();
        assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(new String[]{"--help"}));
        String help = launcher.outText();
        assertTrue(help.contains("Scriptella ETL"));
        assertTrue(help.contains("ETL"));
        assertTrue(help.contains("databases"));
        assertTrue(help.contains("files"));
        assertTrue(help.contains("SQL"));
        assertTrue(help.contains("--help"));
        assertTrue(help.contains("--version"));
        assertTrue(help.contains("--debug"));
        assertTrue(help.contains("--quiet"));
        assertTrue(help.contains("--no-stat"));
        assertTrue(help.contains("--no-jmx"));
        assertTrue(help.contains("--template"));
        assertFalse(help.toLowerCase().contains("agent"));
        assertFalse(help.toLowerCase().contains(" ai"));
        assertFalse(help.toLowerCase().contains("artificial intelligence"));

        launcher.reset();
        assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(new String[]{"-h"}));
        assertTrue(launcher.outText().contains("Scriptella ETL"));

        launcher.reset();
        assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(new String[]{"-help"}));
        assertTrue(launcher.outText().contains("Scriptella ETL"));
    }

    public void testVersionOptions() {
        CapturingLauncher launcher = new CapturingLauncher();
        assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(new String[]{"--version"}));
        assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(new String[]{"-v"}));
        assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(new String[]{"-version"}));
    }

    public void testCanonicalAndLegacyOptionsRecognized() {
        final boolean[] nostat = {false};
        final boolean[] nojmx = {false};
        final boolean[] executed = {false};
        final boolean[] templateInvoked = {false};

        EtlLauncher launcher = new EtlLauncher() {
            @Override
            public void setNoStat(boolean suppressStatistics) {
                nostat[0] = suppressStatistics;
            }

            @Override
            public void setNoJmx(boolean noJmx) {
                nojmx[0] = noJmx;
            }

            @Override
            protected boolean isFile(File file) {
                return true;
            }

            @Override
            public void execute(File file) {
                executed[0] = true;
            }

            @Override
            protected ErrorCode template(List<String> args) {
                templateInvoked[0] = true;
                return ErrorCode.OK;
            }
        };

        assertEquals(EtlLauncher.ErrorCode.OK,
                launcher.launch(new String[]{"--debug", "--quiet", "--no-stat", "--no-jmx", "etl.xml"}));
        assertTrue(nostat[0]);
        assertTrue(nojmx[0]);
        assertTrue(executed[0]);

        nostat[0] = false;
        nojmx[0] = false;
        executed[0] = false;
        assertEquals(EtlLauncher.ErrorCode.OK,
                launcher.launch(new String[]{"-debug", "-quiet", "-nostat", "-nojmx", "etl.xml"}));
        assertTrue(nostat[0]);
        assertTrue(nojmx[0]);
        assertTrue(executed[0]);

        assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(new String[]{"--template"}));
        assertTrue(templateInvoked[0]);
        templateInvoked[0] = false;
        assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(new String[]{"-template"}));
        assertTrue(templateInvoked[0]);
    }

    public void testPrefixOptionsRejected() {
        final boolean[] executed = {false};
        CapturingLauncher launcher = new CapturingLauncher() {
            @Override
            protected boolean isFile(File file) {
                return true;
            }

            @Override
            public void execute(File file) {
                executed[0] = true;
            }
        };

        String[] prefixes = {"-hello", "-quietly", "-version-extra", "-debugX", "-templateX", "-nostatX"};
        for (String option : prefixes) {
            executed[0] = false;
            launcher.reset();
            assertEquals("Expected unrecognized for " + option,
                    EtlLauncher.ErrorCode.UNRECOGNIZED_OPTION,
                    launcher.launch(new String[]{option, "etl.xml"}));
            assertTrue(launcher.errText().contains("Unrecognized option " + option));
            assertEquals("", launcher.outText());
            assertFalse("ETL must not run for " + option, executed[0]);
        }
    }

    public void testUnknownOptionDoesNotExecute() {
        final List<String> files = new ArrayList<String>();
        CapturingLauncher launcher = new CapturingLauncher() {
            @Override
            protected boolean isFile(File file) {
                return true;
            }

            @Override
            public void execute(File file) {
                files.add(file.getName());
            }
        };
        assertEquals(EtlLauncher.ErrorCode.UNRECOGNIZED_OPTION,
                launcher.launch(new String[]{"etl.xml", "--unknown"}));
        assertTrue(files.isEmpty());
        assertTrue(launcher.errText().contains("Unrecognized option --unknown"));
    }

    public void testMultiFileExecutionAndExitCodes() {
        final List<String> files = new ArrayList<String>();
        final boolean[] failSecond = {false};

        EtlLauncher launcher = new EtlLauncher() {
            @Override
            protected boolean isFile(File file) {
                return file.getName().indexOf("_nofile_") < 0;
            }

            @Override
            public void execute(File file) throws EtlExecutorException {
                files.add(file.getName());
                if (failSecond[0] && files.size() == 2) {
                    // SystemException is treated as a normal ETL failure, not a possible bug.
                    throw new EtlExecutorException(new SystemException("forced failure"));
                }
            }
        };

        assertEquals(EtlLauncher.ErrorCode.OK,
                launcher.launch(new String[]{"one.etl.xml", "two.etl.xml"}));
        assertEquals(2, files.size());
        assertEquals("one.etl.xml", files.get(0));
        assertEquals("two.etl.xml", files.get(1));

        files.clear();
        failSecond[0] = true;
        assertEquals(EtlLauncher.ErrorCode.FAILED,
                launcher.launch(new String[]{"one.etl.xml", "two.etl.xml"}));
        assertEquals(2, files.size());
    }

    public void testFile() {
        EtlLauncher launcher = new EtlLauncher();
        assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(new String[]{
                getBasedir() + "src/test/scriptella/tools/launcher/EtlLauncherTest"}));
    }

    /**
     * Tests if JMX monitoring is enabled during execution.
     */
    public void testJmx() throws FileNotFoundException, MalformedURLException, MalformedObjectNameException {
        final EtlLauncher launcher = new EtlLauncher();
        final String fileName = getBasedir() + "src/test/scriptella/tools/launcher/EtlLauncherTestJmx";
        URL u = IOUtils.toUrl(launcher.resolveFile(null, fileName));
        final ObjectName mbeanName = new ObjectName("scriptella:type=etl,url=" + ObjectName.quote(u.toString()));
        final MBeanServer srv = ManagementFactory.getPlatformMBeanServer();
        Callable r = new Callable() {
            public String call() throws Exception {
                try {
                    final Number n = (Number) srv.getAttribute(
                            mbeanName,
                            "ExecutedStatementsCount");
                    assertEquals(2, n.intValue());
                } catch (Exception e) {
                    fail(e.getMessage());
                }
                //Check if cancellation is working
                srv.invoke(mbeanName, "cancel", null, null);
                return "";
            }
        };
        launcher.setProperties(Collections.singletonMap("callback", r));

        assertEquals(EtlLauncher.ErrorCode.FAILED, launcher.launch(new String[]{fileName}));
        assertFalse(srv.isRegistered(mbeanName));
    }

    /**
     * Tests -t option
     */
    public void testTemplate() {
        EtlLauncher etl = new EtlLauncher();
        TemplateManagerTest.TestTemplate.created = false;
        etl.launch(new String[]{"-t", "TemplateManagerTest$TestTemplate"});
        assertTrue(TemplateManagerTest.TestTemplate.created);
    }


    public void testNoStat() {

        final boolean[] nostat = {false};
        final boolean[] executed = {false};
        EtlLauncher l = new EtlLauncher() {
            public void setNoStat(boolean suppressStatistics) {
                nostat[0] = suppressStatistics;
            }


            protected boolean isFile(File file) {
                return true;
            }

            public void execute(File file) throws EtlExecutorException {
                executed[0] = true;

            }
        };
        l.launch(new String[]{"-d", "-nostat"});
        assertTrue("-nostat does not work", nostat[0]);
        assertTrue("script was not executed", executed[0]);
    }

    /**
     * Fix for running inside different environments Maven/Ant or IDE when current directory is different.
     *
     * @return absolute path to basedir directory if basedir property is specified, otherwise empty string
     */
    private String getBasedir() {
        //If running inside Maven the basedir property is set,
        // otherwise assume we run inside IDE/Maven with current directory set to root project
        String basedir = System.getProperty("basedir");
        return basedir != null ? new File(basedir).getAbsolutePath() + '/' : "tools/";
    }

    /**
     * Launcher that captures stdout/stderr for assertions.
     */
    private static class CapturingLauncher extends EtlLauncher {
        private final ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        private final ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
        private final PrintStream out = new PrintStream(outBuf);
        private final PrintStream err = new PrintStream(errBuf);

        @Override
        protected PrintStream getOut() {
            return out;
        }

        @Override
        protected PrintStream getErr() {
            return err;
        }

        void reset() {
            out.flush();
            err.flush();
            outBuf.reset();
            errBuf.reset();
        }

        String outText() {
            out.flush();
            return outBuf.toString();
        }

        String errText() {
            err.flush();
            return errBuf.toString();
        }
    }

}
