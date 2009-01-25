/*
 * Copyright 2006-2007 The Scriptella Project Team.
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


/**
 * A minimized version of {@link ExecutionStatisticsBuilder} optimized for performance.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SilentExecutionStatisticsBuilder extends ExecutionStatisticsBuilder {

    @Override
    /**
     * Called when new element execution started.
     *
     * @param loc element location.
     */
    public void elementStarted(final Location loc, Connection connection) {
        //Do nothing
    }

    @Override
    public void elementExecuted() {
        //Do nothing
    }


    @Override
    public void elementFailed() {
        //Do nothing
    }

}