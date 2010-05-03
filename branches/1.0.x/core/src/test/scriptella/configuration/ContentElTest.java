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
package scriptella.configuration;

import scriptella.AbstractTestCase;
import scriptella.execution.EtlExecutor;

import java.util.List;

/**
 * Tests for {@link scriptella.configuration.ContentEl}.
 */
public class ContentElTest extends AbstractTestCase {
    /**
     * Tests if {@link ContentEl#toString()} returns a location.
     */
    public void testToString() {
        EtlExecutor ex = newEtlExecutor();
        List<ScriptingElement> elements = ex.getConfiguration().getScriptingElements();
        ScriptingElement element = elements.get(0);
        assertEquals("/etl/script[1]", element.getContent().toString());
        QueryEl q = (QueryEl) elements.get(1);
        assertEquals("/etl/query[1]", q.getContent().toString());
        element=q.getChildScriptinglElements().get(0);
        assertEquals("/etl/query[1]/script[1]", element.getContent().toString());
    }

    /**
     * Tests if {@link scriptella.configuration.ContentEl#open()} returns a correct text.
     */
    public void testContent() {
        EtlExecutor ex = newEtlExecutor();
        List<ScriptingElement> elements = ex.getConfiguration().getScriptingElements();
        ScriptingElement element = elements.get(0);
        assertEquals("Script", asString(element.getContent()));
    }


}
