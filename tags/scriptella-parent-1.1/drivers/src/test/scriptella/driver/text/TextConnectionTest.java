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
package scriptella.driver.text;

import scriptella.AbstractTestCase;
import scriptella.configuration.MockConnectionEl;
import scriptella.configuration.StringResource;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.MockDriverContext;
import scriptella.spi.MockParametersCallbacks;

/**
 * Tests for {@link scriptella.driver.text.TextConnection}.
 */
public class TextConnectionTest extends AbstractTestCase {
    /**
     * Test console output
     */
    public void testNoUrl() {
        ConnectionParameters p = new ConnectionParameters(new MockConnectionEl(), MockDriverContext.INSTANCE);
        TextConnection c = new TextConnection(p);
        c.executeScript(new StringResource("Console Output"), MockParametersCallbacks.NULL);
        c.close();
    }
}
