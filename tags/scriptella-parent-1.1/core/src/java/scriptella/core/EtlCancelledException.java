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
package scriptella.core;

/**
 * Thrown to indicate ETL interruption.
 * <p>This exception is handled by a core execution engine and should be
 * propagated by drivers.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlCancelledException extends SystemException {
    public EtlCancelledException() {
        super("ETL Cancelled");
    }

    /**
     * A helper method which check if the current thread is interrupted
     * @throws EtlCancelledException if ETL operation is cancelled.
     */
    public static void checkEtlCancelled() throws EtlCancelledException {
        if (Thread.interrupted()) {
            throw new EtlCancelledException();
        }
    }
}
