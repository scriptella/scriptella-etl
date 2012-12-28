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
package scriptella.driver.hsqldb;

import scriptella.AbstractTestCase;
import scriptella.execution.EtlExecutorException;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

/**
 * Integration test for JDBC connection.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class LobsITest extends AbstractTestCase {
    public void test() throws EtlExecutorException {
        //For now just a smoke test
        newEtlExecutor().execute();
        String tmpDirName = System.getProperty("java.io.tmpdir");
        File[] files = new File(tmpDirName).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("blob_") && name.endsWith(".tmp");
            }
        });
        if (files.length>0) {
            fail("Blob temp files must be removed after ETL completes, but the following files were found "+
                    Arrays.toString(files));
        }
    }
}
