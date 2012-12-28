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
package scriptella.execution;

import scriptella.configuration.Location;
import scriptella.spi.Connection;

import java.util.ArrayList;
import java.util.Date;


/**
 * A builder for execution statistics.
 * <p>This class collects runtime ETL execution statistics, the usage contract is the following:</p>
 * <ul>
 * <li>{@link #etlStarted()} invoked on ETL start.
 * <li>Call {@link #elementStarted(Location,Connection)} before executing an element.
 * <li>Call {@link #elementExecuted()} or {@link #elementFailed()} after executing an element.
 * <li>{@link #etlComplete()} invoked when ETL completes.
 * <li>{@link #getStatistics() Obtain statistics} after ETL completes.
 * </ul>
 * <em>Notes:</em>
 * <ul>
 * <li>Start/End element callbacks sequence must comply with executing elements position in a XML file.
 * <li>This class is not thread safe.
 * </ul>
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ExecutionStatisticsBuilder {
    private ExecutionStatistics executionStatistics;

    //Execution stack for nested elements
    protected ElementsStack executionStack = new ElementsStack();

    /**
     * Called when new element execution started.
     *
     * @param loc element location.
     */
    public void elementStarted(final Location loc, Connection connection) {
        ExecutionStatistics.ElementInfo ei = getInfo(loc);
        executionStack.push(ei);
        ei.statementsOnStart = connection.getExecutedStatementsCount();
        ei.connection = connection;
        ei.started = System.nanoTime();
    }

    /**
     * This method is called when element has been executed.
     */
    public void elementExecuted() {
        setElementState(true);
    }

    /**
     * Invoked on ETL start
     */
    public void etlStarted() {
        executionStatistics = new ExecutionStatistics();
        executionStatistics.setStarted(new Date());
    }

    /**
     * Invoked on ETL completion.
     */
    public void etlComplete() {
        if (executionStatistics == null) {
            throw new IllegalStateException("etlStarted not called");
        }

        //assume that statistics is obtained immediately after the execution
        executionStatistics.setFinished(new Date());
    }


    /**
     * Calculates execution time and statements number for the completed element.
     */
    private void setElementState(boolean ok) {
        ExecutionStatistics.ElementInfo ended = executionStack.pop();
        long ti = System.nanoTime() - ended.started;
        ended.workingTime += ti; //increase the total working time for element
        if (ended.workingTime < 0) {
            ended.workingTime = 0;
        }
        final Connection con = ended.connection;
        ended.connection = null; //clear the connection to avoid leaks
        long conStatements = con.getExecutedStatementsCount();
        long elStatements = conStatements - ended.statementsOnStart;
        if (ok) {
            ended.okCount++;
        } else {
            ended.failedCount++;
        }

        if (elStatements > 0) {
            ended.statements += elStatements;
            executionStatistics.statements += elStatements;
        }

        //Exclude this element time from parent elements
        //Also find the parent elements with the same connection and decrement their number of statements
        for (int i = executionStack.size() - 1; i >= 0; i--) {
            final ExecutionStatistics.ElementInfo parent = executionStack.get(i);
            parent.workingTime -= ti;
            if (parent.connection == con) { //if the same objects
                parent.statementsOnStart += elStatements;
            }
        }

    }

    public void elementFailed() {
        setElementState(false);
    }

    private ExecutionStatistics.ElementInfo getInfo(final Location loc) {
        if (executionStatistics == null) {
            throw new IllegalStateException("etlStarted must be invoked prior to calling this method");
        }
        ExecutionStatistics.ElementInfo ei = executionStatistics.elements.get(loc.getXPath());

        if (ei == null) {
            ei = new ExecutionStatistics.ElementInfo();
            ei.id = loc.getXPath();
            executionStatistics.elements.put(loc.getXPath(), ei);
        }

        return ei;
    }

    public ExecutionStatistics getStatistics() {
        return executionStatistics;
    }

    /**
     * A non-synchronized faster replacement for {@link java.util.Stack}.
     */
    static final class ElementsStack extends ArrayList<ExecutionStatistics.ElementInfo> {
        public ExecutionStatistics.ElementInfo pop() {
            return remove(size() - 1);
        }

        public void push(ExecutionStatistics.ElementInfo element) {
            add(element);
        }
    }
}
