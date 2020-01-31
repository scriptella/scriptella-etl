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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class ShellCommandRunnerTest extends AbstractTestCase {

    public void testExec() throws IOException, ExecutionException, InterruptedException {
        StringReader outReader = new StringReader("Line1\nLine2");
        StringReader errReader = new StringReader("ShellCommandRunnerTest: ignore this line - just for test");
        StringWriter out = new StringWriter();
        ShellCommandRunnerTestable sc = new ShellCommandRunnerTestable(new BufferedWriter(out), outReader, errReader);
        sc.exec("test_command");
        assertTrue(Arrays.equals(new String[] {"", "test_command"}, sc.lastArgs));
        sc.waitForAndCheckExceptions();

        assertTrue("Two lines are expected, but was " + out, out.toString().matches("Line1[\\r\\n]+Line2[\\r\\n]+"));
        try {
            errReader.read();
            fail("stderr must be fully consumed and the stream closed");
        } catch (IOException expected) {
        }
    }

    class ShellCommandRunnerTestable extends ShellCommandRunner {
        String[] lastArgs;

        public ShellCommandRunnerTestable(final BufferedWriter out, Reader procOutputReader, Reader procErrorReader) {
            super(new String[] {""}, out);
            this.procOutputReader = new BufferedReader(procOutputReader);
            this.procErrReader = new BufferedReader(procErrorReader);
        }

        @Override
        protected void execAndInitReaders(final String[] args) {
            lastArgs = args.clone();
        }

        @Override
        protected void waitForProc() throws InterruptedException {
        }
    }
}