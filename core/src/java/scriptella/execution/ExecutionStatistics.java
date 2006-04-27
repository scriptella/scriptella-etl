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

import java.util.*;


/**
 * Represents script execution statistics
 * <p>The statistics is groupped by script elements i.e. sql script or query.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ExecutionStatistics {
    int statements;
    Map<String, ElementInfo> elements = new LinkedHashMap<String, ElementInfo>();
    Map<String, CategoryInfo> categories = new TreeMap<String, CategoryInfo>();

    /**
     * @return number of statements executed for all elements.
     */
    public int getExecutedStatementsCount() {
        return statements;
    }

    public Collection<ElementInfo> getElements() {
        return elements.values();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("Executed ");

        final Set<Map.Entry<String, CategoryInfo>> entries = categories.entrySet();

        for (Iterator<Map.Entry<String, CategoryInfo>> it = entries.iterator();
             it.hasNext();) {
            Map.Entry<String, CategoryInfo> entry = it.next();
            sb.append(entry.getValue().count);
            sb.append(' ').append(entry.getKey());

            if (it.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append('\n');

        for (ElementInfo ei : getElements()) {
            sb.append(ei.id).append(":");

            if (ei.statements > 0) {
                sb.append(' ').append(ei.statements).append(" statements.");
            }

            if (ei.okCount > 0) {
                sb.append(" Succesfully executed ").append(ei.okCount)
                        .append(" of ").append(ei.failedCount + ei.okCount)
                        .append(" time(s).");
            } else {
                sb.append(" Failed ").append(ei.failedCount).append(" time(s).");
            }

            sb.append('\n');
        }

        return sb.toString();
    }

    static class CategoryInfo {
        int count;
    }

    public static class ElementInfo {
        int okCount;
        int statements;
        int failedCount;
        String id;

        public int getSuccessfulExecutionCount() {
            return okCount;
        }

        public int getFailedExecutionCount() {
            return failedCount;
        }

        /**
         * Returns total number of executed statements for this element.
         * <p><b>Note:</b> execution in a loop affects total number,
         * i.e. StatementsCount=loop_count*sql_statements_count
         * @return number of executed statements.
         */
        public int getStatementsCount() {
            return statements;
        }

        public String getId() {
            return id;
        }
    }
}
