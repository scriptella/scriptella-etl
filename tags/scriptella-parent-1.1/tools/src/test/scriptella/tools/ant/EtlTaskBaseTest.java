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

import org.apache.tools.ant.Project;
import scriptella.AbstractTestCase;

/**
 * Tests for {@link scriptella.tools.ant.EtlTaskBase}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlTaskBaseTest extends AbstractTestCase {
    public void test() {
        EtlTaskBase t = new EtlTaskBase();
        //Just a smoke test
        t.setupLogging();
        t.resetLogging();
        Project prj = new Project();
        t.setProject(prj);
        prj.setProperty("_a", "1");

        //Not test inheritAll. By default inheritAll=true
        assertTrue("1".equals(t.getProperties().get("_a")));
        t.setInheritAll(false);
        assertFalse(t.getProperties().containsKey("_a"));
    }
}
