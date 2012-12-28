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

import java.util.Date;

/**
 * JMX MBean interface for ETL task.
 * <p>This interface specifies attributes and operations available via JMX.
 * <p>The ETL mbeans have the following naming convention:
 * <code>
 * <pre>
 * scriptella:type=etl,url=&lt;ETL_XML_FILE_URL&gt;[,n=&lt;COLLISION_ID&gt;]
 * </pre>
 * The collision ID is appended only if the same file is executed simultaneously.
 * </code>
 *
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public interface JmxEtlManagerMBean {
    /**
     * Returns the number of executed statements by all connections of the ETL task.
     * @return non-negative number of executed statements.
     */
    long getExecutedStatementsCount();

    /**
     * Returns the date/time when ETL was started.
     * @return date/time.
     */
    Date getStartDate();

    /**
     * Returns the throughput of the managed ETL task.
     * @return statements/sec throughput or 0 if undefined.
     */
    double getThroughput();

    /**
     * Cancels the managed ETL task.
     */
    void cancel();
}
