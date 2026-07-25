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
package scriptella.driver.velocity;

import org.apache.velocity.runtime.log.LogSystem;
import scriptella.AbstractTestCase;
import scriptella.configuration.StringResource;
import scriptella.spi.MockParametersCallbacks;

import java.io.ByteArrayOutputStream;

/**
 * Characterization of the Velocity 1.x engine integration used by Scriptella.
 * <p>A Velocity 1.7 packaging upgrade must keep LogSystem wiring and evaluate
 * contracts intact. Velocity 2.x is out of scope for 1.4.
 *
 * @author Scriptella Project Team
 */
public class VelocityEngineContractTest extends AbstractTestCase {

    public void testLogSystemConstantsStable() {
        assertEquals(0, LogSystem.DEBUG_ID);
        assertEquals(1, LogSystem.INFO_ID);
        assertEquals(2, LogSystem.WARN_ID);
        assertEquals(3, LogSystem.ERROR_ID);
        assertNotNull(VelocityConnection.LOG_SYSTEM);
    }

    public void testEvaluateSubstitutesParameters() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        VelocityConnection c = VelocityConnectionTest.createConnection(out);
        c.executeScript(new StringResource("Hello $name!"), MockParametersCallbacks.SIMPLE);
        c.close();
        assertEquals("Hello *name*!", out.toString());
    }

    public void testQueryNotSupported() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        VelocityConnection c = VelocityConnectionTest.createConnection(out);
        try {
            c.executeQuery(new StringResource("unused"), MockParametersCallbacks.NULL, null);
            fail("Velocity queries are not supported");
        } catch (UnsupportedOperationException e) {
            // expected
        } finally {
            c.close();
        }
    }
}
