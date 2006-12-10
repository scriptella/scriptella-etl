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
import scriptella.configuration.QueryEl;
import scriptella.configuration.ScriptEl;

import java.util.Collection;
import java.util.Map;


/**
 * Tests for {@link ExecutionStatistics} and {@link ExecutionStatisticsBuilder}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ExecutionStatisticsTest extends DBTestCase {
    public void test() throws EtlExecutorException {
        final EtlExecutor se = newEtlExecutor();
        final ExecutionStatistics s = se.execute();
        Map<String, Integer> cats = s.getCategoriesStatistics();
        assertEquals(2, cats.size());
        assertEquals(3, cats.get(ScriptEl.TAG_NAME).intValue());
        assertEquals(2, cats.get(QueryEl.TAG_NAME).intValue());
        assertEquals(12, s.getExecutedStatementsCount()); //4+2+2+1+3

        final Collection<ExecutionStatistics.ElementInfo> elements = s.getElements();

        for (ExecutionStatistics.ElementInfo info : elements) {
            assertTrue("Negative working time: "+info.getWorkingTime(), info.getWorkingTime()>=0);
            if ("/etl[1]/script[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(4, info.getStatementsCount());
            } else if ("/etl[1]/query[1]/query[1]/script[1]".equals(
                    info.getId())) {
                assertEquals(2, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(2, info.getStatementsCount()); //1 statement executed 2 times
            } else if ("/etl[1]/query[1]/query[1]".equals(info.getId())) {
                assertEquals(2, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(2, info.getStatementsCount());
            } else if ("/etl[1]/query[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(1, info.getStatementsCount());
            } else if ("/etl[1]/script[2]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(3, info.getStatementsCount());
            } else {
                fail("Unrecognized statistic element " + info.getId());
            }
        }
    }

    public void test2() throws EtlExecutorException {
        final EtlExecutor se = newEtlExecutor(
                "ExecutionStatisticsTest2.xml");
        final ExecutionStatistics s = se.execute();
        Map<String, Integer> cats = s.getCategoriesStatistics();
        assertEquals(2, cats.size());
        assertEquals(4, cats.get("script").intValue());
        assertEquals(1, cats.get("query").intValue());

        final Collection<ExecutionStatistics.ElementInfo> elements = s.getElements();

        for (ExecutionStatistics.ElementInfo info : elements) {
            if ("/etl[1]/script[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else if ("/etl[1]/query[1]/script[1]".equals(info.getId())) {
                assertEquals(0, info.getSuccessfulExecutionCount());
                assertEquals(2, info.getFailedExecutionCount());
            } else if ("/etl[1]/query[1]/script[2]".equals(info.getId())) {
                assertEquals(2, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else if ("/etl[1]/query[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else if ("/etl[1]/script[2]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else {
                fail("Unrecognized statistic element " + info.getId());
            }
        }
    }

    public void test3() throws EtlExecutorException {
        final EtlExecutor se = newEtlExecutor(
                "ExecutionStatisticsTest3.xml");
        final ExecutionStatistics s = se.execute();
        Map<String, Integer> cats = s.getCategoriesStatistics();
        assertEquals(2, cats.size());
        assertEquals(2, cats.get("script").intValue());
        assertEquals(2, cats.get("query").intValue());
        assertEquals(9, s.getExecutedStatementsCount()); //4+2+2+1

        final Collection<ExecutionStatistics.ElementInfo> elements = s.getElements();

        for (ExecutionStatistics.ElementInfo info : elements) {
            if ("/etl[1]/script[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(4, info.getStatementsCount());
            } else if ("/etl[1]/query[1]/query[1]/script[1]".equals(
                    info.getId())) {
                assertEquals(2, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(2, info.getStatementsCount()); //1 statement executed 2 times
            } else if ("/etl[1]/query[1]/query[1]".equals(info.getId())) {
                assertEquals(2, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(2, info.getStatementsCount());
            } else if ("/etl[1]/query[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(1, info.getStatementsCount());
            } else {
                fail("Unrecognized statistic element " + info.getId());
            }
        }
    }

}
