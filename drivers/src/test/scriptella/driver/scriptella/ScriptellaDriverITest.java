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
package scriptella.driver.scriptella;

import scriptella.AbstractTestCase;
import scriptella.driver.spring.SpringDriverTest;
import scriptella.execution.EtlExecutorException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Integration test for {@link scriptella.driver.scriptella.Driver}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptellaDriverITest extends AbstractTestCase {
    private static final Logger logger = Logger.getLogger(SpringDriverTest.class.getName());
    public static String global = "";

    public void test() throws EtlExecutorException {
        try {
            newEtlExecutor().execute();
        } catch (EtlExecutorException e) {
            // TODO remove this code once #2 is fixed. Simply checks that the error is caused by Nashorn issue
            StringWriter stackTrace = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stackTrace);
            e.printStackTrace(printWriter);
            printWriter.close();
            if (stackTrace.toString().contains("jdk.nashorn.api.scripting.NashornScriptEngine")) {
                logger.log(Level.SEVERE, "Test failure due to incomplete support for JDK8 Nashorn engine. " +
                        "Suppressing the failure until https://github.com/scriptella/scriptella-etl/issues/2 is fixed", e);
                return;
            } else {
                throw e;
            }
        }
        assertEquals("file2_file1.xml_visible\nfile1.xml_visible\nfile2_", global);
    }

}
