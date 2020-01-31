/*
 * Copyright 2006-2020 The Scriptella Project Team.
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
package scriptella.driver.shell;

import scriptella.AbstractTestCase;
import scriptella.spi.MockConnectionParameters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ShellConnectionParametersTest extends AbstractTestCase {

    public void testGetOsBehavior() {
        Map<String, String> props = new HashMap<>();
        props.put("os_behavior", "windows");
        ShellConnectionParameters p = new ShellConnectionParameters(new MockConnectionParameters(props, null));
        assertEquals(ShellOs.WINDOWS, p.getOsBehavior());
        assertEquals("windows", p.getDialectIdentifier().getName());
    }

    public void testGetShellCommandArgs() {
        Map<String, String> props = new HashMap<>();
        props.put("os_behavior", "mac");
        ShellConnectionParameters p = new ShellConnectionParameters(new MockConnectionParameters(props, null));
        assertEquals(ShellOs.MAC, p.getOsBehavior());
        assertEquals("/bin/sh", p.getShellCommandArgs()[0]);
    }

    public void testGetShellCommandArgsOverride() {
        Map<String, String> props = new HashMap<>();
        props.put("os_behavior", "mac");
        props.put("shell_cmd", "/bin/supershell,--command");
        ShellConnectionParameters p = new ShellConnectionParameters(new MockConnectionParameters(props, null));
        assertEquals(ShellOs.MAC, p.getOsBehavior());
        String[] arr = p.getShellCommandArgs();
        assertTrue("Unexpected result " + Arrays.toString(arr),
                Arrays.equals(new String[] {"/bin/supershell", "--command"}, arr));
    }
}