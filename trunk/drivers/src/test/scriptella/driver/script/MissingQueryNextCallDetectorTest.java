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

package scriptella.driver.script;

import scriptella.AbstractTestCase;
import scriptella.configuration.StringResource;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;

/**
 * @author fkupolov
 */
public class MissingQueryNextCallDetectorTest extends AbstractTestCase implements QueryCallback {
    public void test() {
        Resource r1 = new StringResource("Has query .  next");
        Resource r2 = new StringResource("test");

        ParametersCallbackMap m = new ParametersCallbackMap(MockParametersCallbacks.NAME, this);
        MissingQueryNextCallDetector d = new MissingQueryNextCallDetector(m, r1);
        assertFalse("Warning should not be reported for a script containing query.next", d.detectMissingQueryNextCall());
        m.next();
        assertFalse("Warning should not be reported for a script containing query.next", d.detectMissingQueryNextCall());

        m = new ParametersCallbackMap(MockParametersCallbacks.NAME, this);
        d = new MissingQueryNextCallDetector(m, r2);
        assertTrue("Warning should be reported for a script not containing query.next", d.detectMissingQueryNextCall());
        m.next();
        assertFalse("Warning should not be reported for a script after query.next() was called", d.detectMissingQueryNextCall());
    }

    @Override
    public void processRow(ParametersCallback parameters) {
    }

}
