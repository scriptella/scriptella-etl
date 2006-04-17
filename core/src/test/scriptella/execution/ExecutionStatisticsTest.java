/*
 * Copyright 2006 The Scriptella Project Team.
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
package scriptella.execution;

import scriptella.DBTestCase;

import java.util.Collection;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ExecutionStatisticsTest extends DBTestCase {
    public void test() throws ScriptsExecutorException {
        final ScriptsExecutor se = newScriptsExecutor();
        final ExecutionStatistics s = se.execute();
        assertEquals(2, s.categories.size());

        final Collection<ExecutionStatistics.ElementInfo> elements = s.getElements();

        for (ExecutionStatistics.ElementInfo info : elements) {
            if ("/scripts[1]/script[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else if ("/scripts[1]/query[1]/query[1]/script[1]".equals(
                    info.getId())) {
                assertEquals(2, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else if ("/scripts[1]/query[1]/query[1]".equals(info.getId())) {
                assertEquals(2, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else if ("/scripts[1]/query[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else if ("/scripts[1]/script[2]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else {
                fail("Unrecognized statistic element " + info.getId());
            }
        }
    }

    public void test2() throws ScriptsExecutorException {
        final ScriptsExecutor se = newScriptsExecutor(
                "execution/ExecutionStatisticsTest2.xml");
        final ExecutionStatistics s = se.execute();
        assertEquals(2, s.categories.size());

        final Collection<ExecutionStatistics.ElementInfo> elements = s.getElements();

        for (ExecutionStatistics.ElementInfo info : elements) {
            if ("/scripts[1]/script[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else if ("/scripts[1]/query[1]/script[1]".equals(info.getId())) {
                assertEquals(0, info.getSuccessfulExecutionCount());
                assertEquals(2, info.getFailedExecutionCount());
            } else if ("/scripts[1]/query[1]/script[2]".equals(info.getId())) {
                assertEquals(2, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else if ("/scripts[1]/query[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else if ("/scripts[1]/script[2]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else {
                fail("Unrecognized statistic element " + info.getId());
            }
        }
    }
}
