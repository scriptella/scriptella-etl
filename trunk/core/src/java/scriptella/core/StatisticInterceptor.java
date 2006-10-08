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
package scriptella.core;

import scriptella.configuration.Location;
import scriptella.execution.ExecutionStatisticsBuilder;


/**
 * Collects execution statistics.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class StatisticInterceptor extends ElementInterceptor {
    private Location location;

    public StatisticInterceptor(ExecutableElement next, Location location) {
        super(next);
        this.location = location;
    }

    public void execute(final DynamicContext ctx) {
        boolean ok = false;
        final ExecutionStatisticsBuilder statisticsBuilder = ctx.getGlobalContext().getStatisticsBuilder();
        try {
            statisticsBuilder.elementStarted(location);
            executeNext(ctx);
            ok = true;
        } finally {
            if (ok) {
                Integer count = STATEMENTS_INFO.get();//Obtain statistics of executed statements (if any)
                STATEMENTS_INFO.remove(); //Clear threalocal state
                
                if (count == null) { //no information available
                    statisticsBuilder.elementExecuted();
                } else {
                    statisticsBuilder.elementExecuted(count);
                }
            } else {
                STATEMENTS_INFO.remove(); //Clear threalocal state
                statisticsBuilder.elementFailed();
            }
        }

    }

    public static ExecutableElement prepare(
            final ExecutableElement next, final Location location) {
        return new StatisticInterceptor(next, location);
    }

    /**
     * Updates statistics on number of executed statements for current element.
     *
     * @param statements number of executed statements
     */
    public static void statementsExecuted(int statements) {
        STATEMENTS_INFO.set(statements);
    }

    private static final ThreadLocal<Integer> STATEMENTS_INFO = new ThreadLocal<Integer>();
}
