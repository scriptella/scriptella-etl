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

import scriptella.driver.text.TextConnectionParameters;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.DialectIdentifier;

/**
 * Connection parameters for shell driver.
 *
 * @author Fyodor Kupolov
 * @version 1.1
 */
public class ShellConnectionParameters extends TextConnectionParameters {
    private ShellOs osBehavior;
    private DialectIdentifier dialectIdentifier;
    private String[] shellCommandArgs;

    ShellConnectionParameters(ConnectionParameters parameters) {
        super(parameters);
        // Use os behavior param or guess from the running env
        String shellOs = parameters.getStringProperty("os_behavior");
        if (shellOs != null) {
            osBehavior = ShellOs.fromOsNameVersion(shellOs, null);
            dialectIdentifier = new DialectIdentifier(shellOs, null);
        } else {
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            osBehavior = ShellOs.fromOsNameVersion(osName, osVersion);
            dialectIdentifier = new DialectIdentifier(osName, osVersion);
        }
        String shellCmds = parameters.getStringProperty("shell_cmd");
        if (shellCmds != null) {
            shellCommandArgs = shellCmds.split("\\s*,\\s*");
        } else if (osBehavior == ShellOs.LINUX || osBehavior == ShellOs.MAC) {
            shellCommandArgs = new String[] {"/bin/sh", "-c"};
        } else if (osBehavior == ShellOs.WINDOWS) {
            shellCommandArgs = new String[] {"cmd.exe", "/c"};
        }
    }

    ShellOs getOsBehavior() {
        return osBehavior;
    }

    DialectIdentifier getDialectIdentifier() {
        return dialectIdentifier;
    }

    String[] getShellCommandArgs() {
        return shellCommandArgs;
    }
}
