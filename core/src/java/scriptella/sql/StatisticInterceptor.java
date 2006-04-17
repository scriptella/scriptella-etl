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
package scriptella.sql;

import scriptella.configuration.Location;


/**
 * Collects execution statistics.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class StatisticInterceptor extends SQLElementInterceptor {
    private Location location;

    public StatisticInterceptor(SQLExecutableElement next, Location location) {
        super(next);
        this.location = location;
    }

    public void execute(final SQLContext ctx) {
        executeNext(ctx);
        ctx.globalContext.getStatisticsBuilder().elementExecuted(location);
    }

    public static SQLExecutableElement prepare(
            final SQLExecutableElement next, final Location location) {
        return new StatisticInterceptor(next, location);
    }
}