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

import scriptella.DBTestCase;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.File;
import java.io.FileNotFoundException;
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
                return file.getName().indexOf("_nofile_")<0;
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
        EtlLauncher etlLauncher = new EtlLauncher() {
            @Override
            protected boolean isFile(File file) {
                return false;
            }
        };
        assertEquals(EtlLauncher.ErrorCode.FILE_NOT_FOUND, etlLauncher.launch(new String[] {}));


    }

    public void testFile() {
        EtlLauncher launcher  = new EtlLauncher();
        assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(new String[] {"tools/src/test/scriptella/tools/launcher/EtlLauncherTest"}));
    }

    /**
     * Tests if JMX monitoring is enabled during execution.
     */
    public void testJmx() throws FileNotFoundException, MalformedURLException, MalformedObjectNameException {
        final EtlLauncher launcher  = new EtlLauncher();
        final String fileName = "tools/src/test/scriptella/tools/launcher/EtlLauncherTestJmx";
        URL u = launcher.resolveFile(null, fileName).toURL();
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
                srv.invoke(mbeanName, "cancel",null, null);
                return "";
            }
        };
        launcher.setProperties(Collections.singletonMap("callback", r));

        assertEquals(EtlLauncher.ErrorCode.FAILED, launcher.launch(new String[] {fileName}));
        assertFalse(srv.isRegistered(mbeanName));
    }


}
