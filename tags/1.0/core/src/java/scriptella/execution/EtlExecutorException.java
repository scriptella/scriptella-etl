/*
 * Copyright 2006-2009 The Scriptella Project Team.
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
import scriptella.core.EtlCancelledException;
import scriptella.core.ExceptionInterceptor;
import scriptella.expression.Expression;
import scriptella.spi.ProviderException;
import scriptella.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * Thrown on script execution failure
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlExecutorException extends Exception {
    private ProviderException lastProvider;
    private Throwable lastExpression;
    private Location lastElementLocation;
    private String message;
    private boolean cancelled;

    public EtlExecutorException(Throwable cause) {
        super(cause);

        for (Throwable ex = cause; ex != null; ex = ex.getCause()) {
            if (isExpression(ex)) {
                lastExpression = ex;
            }

            if (ex instanceof ProviderException) {
                ProviderException providerEx = (ProviderException) ex;
                lastProvider = providerEx;
            }

            if (ex instanceof ExceptionInterceptor.ExecutionException) {
                lastElementLocation = ((ExceptionInterceptor.ExecutionException) ex).getLocation();
            }

            if (ex instanceof EtlCancelledException || ex instanceof InterruptedException) {
                cancelled = true;
            }
        }

        final StringWriter out = new StringWriter();
        PrintWriter pw = new PrintWriter(out); //Use print writer to handle line separators
        if ((lastProvider==null || lastElementLocation==null) && cause != null && cause.getMessage() != null) {
            pw.println(cause.getMessage());
        }
        if (lastElementLocation != null) {
            pw.print("Location: ");
            pw.println(lastElementLocation);
        }
        if (lastProvider != null) {
            pw.print(lastProvider.getProviderName() + " provider exception: ");
            pw.println(lastProvider.getMessage());

            if (lastProvider.getErrorStatement() != null) {
                pw.print("Error statement: ");
                pw.print(lastProvider.getErrorStatement());
                pw.println();
            }
            pw.println("Error codes: " + lastProvider.getErrorCodes());
            Throwable nativeException = lastProvider.getNativeException();
            if (nativeException != null) {
                pw.print("Driver exception: ");
                pw.print(nativeException.toString());
                pw.println();
            }
        }
        if (lastExpression != null) {
            pw.print("Expression exception: ");
            pw.print(lastExpression.getMessage());
            pw.println();
        }
        this.message = StringUtils.consoleFormat(out.toString());
    }


    public String getMessage() {
        return message;
    }

    public ProviderException getLastProvider() {
        return lastProvider;
    }

    public Throwable getLastExpression() {
        return lastExpression;
    }

    public Location getLastElementLocation() {
        return lastElementLocation;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    private static boolean isExpression(final Throwable cause) {
        return cause instanceof Expression.ParseException ||
                cause instanceof Expression.EvaluationException;
    }

}
