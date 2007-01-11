/*
 * Copyright 2006-2007 The Scriptella Project Team.
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
package scriptella.tools.template;

import scriptella.AbstractTestCase;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link TemplateManager}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class TemplateManagerTest extends AbstractTestCase {

    public void test() throws IOException {
        final Map<String,Writer> files = new HashMap<String, Writer>();
        TemplateManager tm = new TemplateManager() {
            protected Writer newFileWriter(String fileName) {
                files.put(fileName, new StringWriter());
                return files.get(fileName);
            }

            protected boolean checkFile(String name) {
                //etl.xml is absent but properties exist
                if ("etl.xml".equals(name)) {
                    return true;
                } else if ("etl.properties".equals(name)) {
                    return false;
                }
                return true;
            }
        };
        tm.create();
        assertEquals(2, files.size());
        assertTrue(files.containsKey("etl[1].xml"));
        assertTrue(files.containsKey("etl[1].properties"));

    }
}
