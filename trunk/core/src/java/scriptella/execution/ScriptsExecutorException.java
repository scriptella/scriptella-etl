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
import scriptella.expressions.Expression;
import scriptella.sql.ExceptionInterceptor;
import scriptella.sql.JDBCException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;


/**
 * Thrown on script execution failure
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptsExecutorException extends Exception {
    private JDBCException lastJDBC;
    private SQLException lastSQL;
    private Throwable lastExpression;
    private Location lastElementLocation;
    private String message;

    public ScriptsExecutorException(Throwable cause) {
        super(cause);

        for (Throwable ex = cause; ex != null; ex = ex.getCause()) {
            if (isExpression(ex)) {
                lastExpression = ex;
            }

            if (ex instanceof JDBCException) {
                JDBCException jdbcEx = (JDBCException) ex;

                if (jdbcEx.getSql() != null) {
                    lastJDBC = jdbcEx;
                } else if (lastJDBC == null) {
                    lastJDBC = jdbcEx;
                }
            }

            if (ex instanceof SQLException) {
                lastSQL = (SQLException) ex;
            }

            if (ex instanceof ExceptionInterceptor.ExecutionException) {
                lastElementLocation = ((ExceptionInterceptor.ExecutionException) ex).getLocation();
            }
        }

        final StringWriter out = new StringWriter();
        PrintWriter pw = new PrintWriter(out); //Use print writer to handle line separators
        if (cause != null && cause.getMessage() != null) {
            pw.println(cause.getMessage());
        }
        if (lastElementLocation != null) {
            pw.print("Location: ");
            pw.println(lastElementLocation);
        }

        if (lastJDBC != null) {
            pw.print("JDBC exception: ");
            pw.print(lastJDBC.getMessage());

            if (lastJDBC.getSql() != null) {
                pw.print(". Statement: ");
                pw.print(lastJDBC.getSql());
                pw.print(". Parameters: ");
                pw.print(lastJDBC.getParameters());
            }

            pw.println();
        }

        if (lastSQL != null) {
            pw.print("JDBC driver exception: ");
            pw.print(lastSQL.getMessage());
            pw.println();
        }

        if (lastExpression != null) {
            pw.print("Expression exception: ");
            pw.print(lastExpression.getMessage());
            pw.println();
        }
        this.message = out.toString();
    }

    public String getMessage() {
        return message;
    }

    private static boolean isExpression(final Throwable cause) {
        return cause instanceof Expression.ParseException ||
                cause instanceof Expression.EvaluationException;
    }

}
