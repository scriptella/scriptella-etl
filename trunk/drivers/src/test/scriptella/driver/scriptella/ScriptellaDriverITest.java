/*
 * Copyright 2006-2009 The Scriptella Project Team.
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
import scriptella.execution.EtlExecutorException;

/**
 * Integration test for {@link scriptella.driver.scriptella.Driver}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptellaDriverITest extends AbstractTestCase {
    public static String global = "";

    public void test() throws EtlExecutorException {
        newEtlExecutor().execute();
        assertEquals("file2_file1.xml_visible\nfile1.xml_visible\nfile2_", global);
    }

}
