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
package scriptella.tools.launcher;

import scriptella.core.ExceptionInterceptor;
import scriptella.core.SystemException;
import scriptella.execution.EtlExecutorException;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Represents bug report for unexpected conditions.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class BugReport {
    private Throwable throwable;

    /**
     * Creates bug report for throwable.
     *
     * @param throwable
     */
    public BugReport(Throwable throwable) {
        this.throwable = throwable;
    }

    /**
     * @param throwable
     * @return true if throwable is likely to be caused by bug
     */
    public static boolean isPossibleBug(Throwable throwable) {
        if (throwable instanceof ExceptionInterceptor.ExecutionException) {
            //intercepted exceptions should be handled too
            if (throwable.getCause() != null) {
                return isPossibleBug(throwable.getCause());
            } else {
                return true; //intercepted exceptions must have cause
            }
        } else if (throwable instanceof EtlExecutorException) {
            if (throwable.getCause() != null) {
                return isPossibleBug(throwable.getCause());
            } else {
                return true; //EtlExecutorException must have cause
            }
        }
        return !(throwable instanceof SystemException);
    }

    /**
     * @return report content.
     */
    public String toString() {
        //produce bug report
        StringWriter rep = new StringWriter();
        PrintWriter pw = new PrintWriter(rep);
        pw.println("Scriptella bug report. Submit to issue tracker.");
        Package p = BugReport.class.getPackage();
        String version = p != null && p.getImplementationVersion() != null ? p.getImplementationVersion() : "Unknown";
        pw.println("Scriptella version: " + version);
        pw.println("Exception: ");
        throwable.printStackTrace(pw);
        pw.println("Environment: ");
        pw.println(System.getenv());
        pw.println("System properties: ");
        pw.println(System.getProperties());
        pw.println("-----------------------------------------------------------------");
        //todo add information about version, thread states etc.
        return rep.toString();

    }


}
