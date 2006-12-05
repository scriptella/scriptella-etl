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

import scriptella.configuration.Location;

import java.util.Stack;


/**
 * A builder for execution statistics.
 * <p>This class collects runtime ETL execution statistics, the usage contract is the following:</p>
 * <ul>
 * <li>Call {@link #elementStarted(scriptella.configuration.Location)} before executing an element.
 * <li>Call {@link #elementExecuted()} or {@link #elementFailed()} after executing an element.
 * <li>{@link #getStatistics() Obtain statistics} when ETL completes.
 * </ul>
 * <em>Notes:</em>
 * <ul>
 * <li>Start/End element callbacks sequence must comply with executing elements position in a XML file.
*  <li>This class is not thread safe.
 * </ul>
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ExecutionStatisticsBuilder {
    private ExecutionStatistics executionStatistics = new ExecutionStatistics();
    //assume that instantiation time is the start time
    private long started = System.currentTimeMillis();

    //Execution stack for nested elements
    private Stack<ExecutionStatistics.ElementInfo> executionStack = new Stack<ExecutionStatistics.ElementInfo>();

    /**
     * Called when new element execution started.
     *
     * @param loc element location.
     */
    public void elementStarted(final Location loc) {
        ExecutionStatistics.ElementInfo ei = getInfo(loc);
        executionStack.push(ei);
        ei.started=System.nanoTime();
    }

    /**
     * Calls {@link #elementExecuted(int) elementExecuted(0)}
     */
    public void elementExecuted() {
        elementExecuted(0);
    }

    /**
     * This method is called when element has been executed.
     * @param statements number of executed statements.
     */
    public void elementExecuted(final int statements) {
        executionStatistics.statements += statements;

        ExecutionStatistics.ElementInfo ei = getLastStartedElement();
        ei.okCount++;

        if (statements != 0) {
            ei.statements += statements;
        }
    }

    /**
     * Calculates execution time and return the last started element.
     *
     * @return last started element.
     */
    private ExecutionStatistics.ElementInfo getLastStartedElement() {
        ExecutionStatistics.ElementInfo ended = executionStack.pop();
        long ti = System.nanoTime() - ended.started;
        ended.workingTime += ti; //increase the total working time for element
        //Now substract the execution time from parent element.
        //Because query time must not include children
        if (!executionStack.isEmpty()) {
            executionStack.peek().workingTime -= ti;
        }
        return ended;
    }

    public void elementFailed() {
        getLastStartedElement().failedCount++;
    }

    private ExecutionStatistics.ElementInfo getInfo(final Location loc) {
        ExecutionStatistics.ElementInfo ei = executionStatistics.elements.get(loc.getXpath());

        if (ei == null) {
            ei = new ExecutionStatistics.ElementInfo();
            ei.id = loc.getXpath();
            executionStatistics.elements.put(loc.getXpath(), ei);
        }

        return ei;
    }

    public ExecutionStatistics getStatistics() {
        //assume that statistics is obtained immediately after the execution
        executionStatistics.setTotalTime(System.currentTimeMillis() - started);
        return executionStatistics;
    }
}
