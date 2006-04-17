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


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ExecutionStatisticsBuilder {
    private ExecutionStatistics executionStatistics = new ExecutionStatistics();

    public void elementExecuted(final Location loc) {
        elementExecuted(loc, 0);
    }

    public void elementExecuted(final Location loc, final int statements) {
        executionStatistics.statements += statements;

        ExecutionStatistics.ElementInfo ei = getInfo(loc);
        ei.okCount++;

        if (statements != 0) {
            ei.statements = statements;
        }
    }

    public void elementFailed(final Location loc) {
        ExecutionStatistics.ElementInfo ei = getInfo(loc);
        ei.failedCount++;
    }

    private ExecutionStatistics.ElementInfo getInfo(final Location loc) {
        ExecutionStatistics.ElementInfo ei = executionStatistics.elements.get(loc.getXpath());

        if (ei == null) {
            ei = new ExecutionStatistics.ElementInfo();
            ei.id = loc.getXpath();
            executionStatistics.elements.put(loc.getXpath(), ei);
            getCategory(loc.getCategory()).count++; //Increment script's category only once
        }

        return ei;
    }

    private ExecutionStatistics.CategoryInfo getCategory(final String name) {
        ExecutionStatistics.CategoryInfo ci = executionStatistics.categories.get(name);

        if (ci == null) {
            ci = new ExecutionStatistics.CategoryInfo();
            executionStatistics.categories.put(name, ci);
        }

        return ci;
    }

    public ExecutionStatistics getStatistics() {
        return executionStatistics;
    }

    public static void main(final String args[]) {
        Location e1 = new Location("Script #1", "Scripts");
        Location e2 = new Location("Script #2", "Scripts");
        Location e3 = new Location("Query #1", "Queries");

        ExecutionStatisticsBuilder b = new ExecutionStatisticsBuilder();
        b.elementExecuted(e1, 10);
        b.elementExecuted(e1, 10);
        b.elementFailed(e1);
        b.elementExecuted(e3);
        b.elementFailed(e2);

        final ExecutionStatistics s = b.getStatistics();
        System.out.println("s = " + s);
    }
}
