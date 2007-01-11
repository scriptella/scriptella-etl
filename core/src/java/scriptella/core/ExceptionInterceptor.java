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
package scriptella.core;

import scriptella.configuration.Location;


/**
 * Intercepts exceptions thrown by wrapped executable elements.
 * Additionally checks if ETL has been cancelled.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ExceptionInterceptor extends ElementInterceptor {
    private Location location;

    public ExceptionInterceptor(ExecutableElement next, Location location) {
        super(next);
        this.location = location;
    }

    public void execute(final DynamicContext ctx) {
        try {
            EtlCancelledException.checkEtlCancelled();
            executeNext(ctx);
        } catch (ExecutionException e) {
            //do not wrap already intercepted exceptions
            throw e;
        } catch (Exception e) {
            throw new ExecutionException(e, location);
        }
    }

    public static ExecutableElement prepare(
            final ExecutableElement next, final Location loc) {
        return new ExceptionInterceptor(next, loc);
    }

    public static class ExecutionException extends SystemException {
        private Location location;

        public ExecutionException(Throwable cause, Location location) {
            this(location + " failed: " + cause.getMessage(), cause, location);
        }

        public ExecutionException(String message, Throwable cause,
                                  Location location) {
            super(message, cause);
            this.location = location;
        }

        /**
         * @return Failed element location.
         */
        public Location getLocation() {
            return location;
        }
    }
}
