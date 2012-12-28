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
import scriptella.AbstractTestCase;
import scriptella.tools.template.DataMigrator;
import scriptella.tools.template.TemplateManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link scriptella.tools.ant.EtlTemplateTask}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlTemplateTaskTest extends AbstractTestCase {
    private Map<String, ?> props;

    public void test() {
        EtlTemplateTask t = new EtlTemplateTask() {
            @Override//Verify that data migrator template is used
            protected void create(TemplateManager tm, Map<String, ?> properties) throws IOException {
                assertTrue(tm instanceof DataMigrator);
                props = properties;
            }

            @Override//Return mock properties to isolate from Ant
            protected Map<String, ?> getProperties() {
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("a", "AA");
                m.put("b", "BB");
                return m;
            }
        };
        try {
            t.execute();
            fail("Required attribute exception expected");
        } catch (BuildException e) {
            //OK
        }
        t.setName(DataMigrator.class.getSimpleName());
        t.execute();
        assertTrue(props != null && "AA".equals(props.get("a")) && props.size() == 2);
    }

}
