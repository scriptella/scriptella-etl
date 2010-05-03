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
package scriptella.tools.launcher;

import scriptella.AbstractTestCase;
import scriptella.configuration.ConfigurationException;
import scriptella.core.SystemException;

/**
 * Tests for {@link BugReport}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class BugReportTest extends AbstractTestCase {
    public void test() {
        assertFalse(BugReport.isPossibleBug(new ConfigurationException("Just a test")));
        assertFalse(BugReport.isPossibleBug(new ConfigurationException("Just a test")));
        assertFalse(BugReport.isPossibleBug(new SystemException("Just a test")));
        NullPointerException npe = new NullPointerException("Just a test");
        assertTrue(BugReport.isPossibleBug(npe));
        String s = new BugReport(npe).toString();
        assertTrue(s.indexOf(npe.getMessage())>0);

    }
}
