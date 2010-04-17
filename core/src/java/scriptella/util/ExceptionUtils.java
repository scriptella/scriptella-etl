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
package scriptella.util;

import java.sql.SQLException;

/**
 * Utility class to work with throwables.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public final class ExceptionUtils {
    private ExceptionUtils() {
    }

    /**
     * This method checks if throwable has {@link Throwable#getCause() cause}.
     * If cause==null, this method tries to obtain cause based on throwable type,
     * e.g. {@link java.sql.SQLException#getNextException()}
     *
     * @param throwable throwable to find cause.
     * @return cause of throwable.
     */
    public static Throwable getCause(Throwable throwable) {
        if (throwable.getCause() != null) {
            return throwable.getCause();
        }
        if (throwable instanceof SQLException) {
            return ((SQLException) throwable).getNextException();
        }
        return null;
    }

    /**
     * This method throws unchecked throwable, i.e. {@link Error} or {@link RuntimeException}.
     *
     * @param unchecked unchecked throwable.
     */
    public static void throwUnchecked(Throwable unchecked) {
        if (unchecked instanceof Error) {
            throw (Error) unchecked;
        }
        if (unchecked instanceof RuntimeException) {
            throw (RuntimeException) unchecked;
        }
        throw new IllegalStateException("Unchecked throwable expected but was " + unchecked.getClass(), unchecked);
    }

    /**
     * Utility method to ignore non important exceptions.
     * @param throwable throwable to ignore.
     */
    public static void ignoreThrowable(Throwable throwable) {
    }

}
