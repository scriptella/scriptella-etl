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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.FileSet;
import scriptella.AbstractTestCase;

/**
 * Characterization of the Ant API surface used by Scriptella's Ant tasks.
 * <p>An Ant 1.10.x compile dependency bump must keep these types available.
 *
 * @author Scriptella Project Team
 */
public class AntApiContractTest extends AbstractTestCase {

    public void testTaskBaseTypesResolvable() {
        Project project = new Project();
        project.init();

        EtlExecuteTask task = new EtlExecuteTask();
        task.setProject(project);
        task.setTaskName("etl");
        assertEquals("etl", task.getTaskName());

        FileSet fs = new FileSet();
        fs.setProject(project);
        task.addFileset(fs);

        // Types referenced by forked execution path
        assertNotNull(Java.class);
        assertNotNull(DirectoryScanner.class);
        assertNotNull(BuildException.class);
    }

    public void testBuildExceptionIsThrowable() {
        BuildException be = new BuildException("contract");
        assertEquals("contract", be.getMessage());
    }
}
