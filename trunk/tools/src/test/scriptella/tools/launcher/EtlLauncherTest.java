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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        assertEquals(0, etlLauncher.launch(new String[]{"-v"}));
        assertEquals(3, etlLauncher.launch(new String[]{"-nosuchproperty"}));
        assertEquals(0, etlLauncher.launch(new String[]{"-h"}));
        assertEquals(0, etlLauncher.launch(new String[]{}));
        assertEquals(1, files.size());
        assertEquals("etl.xml", files.get(0));
        files.clear();
        assertEquals(2, etlLauncher.launch(new String[]{"_nofile_", "etl.xml"}));
        assertEquals(0, files.size());
    }

    public void testNoEtlFile() {
        EtlLauncher etlLauncher = new EtlLauncher() {
            @Override
            protected boolean isFile(File file) {
                return false;
            }
        };
        assertEquals(2, etlLauncher.launch(new String[] {}));


    }

    public void testFile() {
        EtlLauncher launcher  = new EtlLauncher();
        assertEquals(0, launcher.launch(new String[] {"tools/src/test/scriptella/tools/launcher/EtlLauncherTest"}));
    }

}
