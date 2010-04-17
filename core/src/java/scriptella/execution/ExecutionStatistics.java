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

import scriptella.spi.Connection;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Represents script execution statistics
 * <p>The statistics is groupped by script elements i.e. script or query.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ExecutionStatistics {
    int statements;
    Map<String, ElementInfo> elements = new LinkedHashMap<String, ElementInfo>();
    private Date started;
    private Date finished;
    private static final int MINUTE_MILLIS = 60 * 1000;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;
    static final String DOUBLE_FORMAT_PTR = "0.##";


    /**
     * @return number of statements executed for all elements.
     */
    public int getExecutedStatementsCount() {
        return statements;
    }

    public Collection<ElementInfo> getElements() {
        return elements.values();
    }

    /**
     * Returns the statistics on executed categories, e.g.
     * queries-5times, scripts-10times.
     * <p>Note the category names are xml element names, no plural form used.
     *
     * @return xmlelement->count map .
     */
    public Map<String, Integer> getCategoriesStatistics() {
        Map<String, Integer> res = new HashMap<String, Integer>();
        for (String xpath : elements.keySet()) {
            String elementName = getElementName(xpath);
            Integer cnt = res.get(elementName);
            if (cnt == null) {
                cnt = 0;
            }
            res.put(elementName, ++cnt); //autoboxing works fine on small numbers
        }
        return res;
    }

    public String toString() {
        final Collection<ElementInfo> elements = getElements();
        if (elements.isEmpty()) {
            return "No elements executed";
        }
        StringBuilder sb = new StringBuilder(1024);
        NumberFormat doubleFormat = new DecimalFormat(DOUBLE_FORMAT_PTR);
        sb.append("Executed ");

        Map<String, Integer> cats = getCategoriesStatistics();
        for (Iterator<Map.Entry<String, Integer>> it = cats.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Integer> category = it.next();
            appendPlural(sb, category.getValue(), category.getKey());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        if (statements > 0) {
            sb.append(", ");
            appendPlural(sb, statements, "statement");
        }

        sb.append('\n');

        for (ElementInfo ei : elements) {
            sb.append(ei.id).append(":");

            if (ei.okCount > 0) {
                sb.append(" Element successfully executed");
                if (ei.okCount > 1) {
                    sb.append(' ');
                    appendPlural(sb, ei.okCount, "time");
                }
                if (ei.statements > 0) {
                    sb.append(" (");
                    appendPlural(sb, ei.statements, "statement").append(')');
                }
                sb.append('.');
            }

            if (ei.failedCount > 0) {
                sb.append(" Element failed ");
                appendPlural(sb, ei.failedCount, "time");
                sb.append('.');
            }
            sb.append(" Working time ").append(ei.workingTime / 1000000).append(" milliseconds.");
            //Output throughput
            double throughput = ei.getThroughput();
            if (throughput >= 0) {
                sb.append(" Avg throughput: ").append(doubleFormat.format(throughput)).append(" statements/sec.");
            }
            sb.append('\n');

        }
        long totalTime = getTotalTime();
        if (totalTime >= 0) {
            sb.append("Total working time:");
            appendTotalTimeDuration(totalTime, sb, doubleFormat);
        }
        return sb.toString();
    }

    /**
     * A helper method to get element name from simplified location xpath.
     *
     * @param xpath xpath to get referenced element name.
     * @return element name.
     */
    private static String getElementName(String xpath) {
        int slash = xpath.lastIndexOf('/');
        int br = xpath.lastIndexOf('[');
        return xpath.substring(slash + 1, br);
    }

    private static StringBuilder appendPlural(final StringBuilder sb, final long num, final String singularNoun) {
        sb.append(num).append(' ');
        if (num > 1) { //plural form
            if ("query".equals(singularNoun)) { //exceptions
                sb.append("queries");
            } else { //default rule appends S
                sb.append(singularNoun).append('s');
            }
        } else {
            sb.append(singularNoun); //singular form
        }
        return sb;
    }

    /**
     * Appends total working time, i.e. d h m s ms
     * The leading space is always present.
     *
     * @param timeMillis   time interval in millis.
     * @param sb           output buffer.
     * @param doubleFormat format seconds.
     * @return the modified buffer.
     */
    static StringBuilder appendTotalTimeDuration(final long timeMillis, StringBuilder sb, Format doubleFormat) {
        long time = timeMillis;
        long days = time / DAY_MILLIS;
        if (days > 0) {
            time = time % DAY_MILLIS;
            sb.append(' ');
            appendPlural(sb, days, "day");
        }
        long hours = time / HOUR_MILLIS;
        if (hours > 0) {
            time = time % HOUR_MILLIS;
            sb.append(' ');
            appendPlural(sb, hours, "hour");
        }
        long minutes = time / MINUTE_MILLIS;
        if (minutes > 0) {
            time = time % MINUTE_MILLIS;
            sb.append(' ');
            appendPlural(sb, minutes, "minute");
        }
        double seconds = time / 1000d;
        if (seconds > 0) {
            sb.append(' ');
            sb.append(doubleFormat.format(seconds)).append(" second");
            if (seconds > 1) { //plural form
                sb.append('s');
            }
        }
        return sb;
    }

    /**
     * Total ETL execution time or -1 if ETL hasn't completed.
     *
     * @return ETL execution time in milliseconds.
     */
    public long getTotalTime() {
        return finished != null && started != null ? finished.getTime() - started.getTime() : -1;
    }

    /**
     * Returns date/time when ETL was started.
     *
     * @return ETL start date/time.
     */
    public Date getStartDate() {
        return started == null ? null : (Date) started.clone();
    }

    void setStarted(Date started) {
        this.started = started;
    }

    /**
     * Returns date/time when ETL was completed.
     *
     * @return ETL finish date/time.
     */
    public Date getFinishDate() {
        return finished == null ? null : (Date) finished.clone();
    }

    void setFinished(Date finished) {
        this.finished = finished;
    }


    public static class ElementInfo {
        int okCount;
        Connection connection;
        long statementsOnStart;
        long statements;
        int failedCount;
        long started;
        long workingTime;
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
         *
         * @return number of executed statements.
         */
        public long getStatementsCount() {
            return statements;
        }

        /**
         * Returns the total number of nanoseconds spent while executing this element.
         *
         * @return total working time in nanoseconds.
         */
        public long getWorkingTime() {
            return workingTime;
        }

        /**
         * Returns throughput t=statements/workingTimeSeconds. The
         * throughput has statement/second unit.
         *
         * @return statement/second thoughput or -1 if no statements info available or working time is zero.
         */
        public double getThroughput() {
            return statements <= 0 || workingTime <= 0 ? -1 : 1000000000d * statements / workingTime;
        }

        public String getId() {
            return id;
        }
    }
}
