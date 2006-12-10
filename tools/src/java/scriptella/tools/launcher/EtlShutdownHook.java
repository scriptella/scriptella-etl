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
package scriptella.tools.launcher;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shutdown hook added by {@link EtlLauncher}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class EtlShutdownHook extends Thread {
    private static final Logger LOG = Logger.getLogger(EtlShutdownHook.class.getName());
    private Thread etlThread;
    public EtlShutdownHook() {
        setName("ETL Cancellation Thread");
    }

    /**
     * Registers a system shutdown hook which interrupts ETL working thread on VM exit.
     * <p>If shutdown hook is invoked
     * interrupt is invoked for ETL Thread only if the thread is alive and not interrupted.
     * <p>This method must be invoked by a thread which invokes the
     * {@link scriptella.execution.EtlExecutor}.
     */
    public void register() {
        etlThread = Thread.currentThread();
        try {
            Runtime.getRuntime().addShutdownHook(this);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to add shutdown hook. ETL will not be rolled back on abnormal termination.", e);
        }
    }

    /**
     * Unregisters this system shutdown hook if it has been registered.
     */
    public void unregister() {
        etlThread = null;
        try {
            Runtime.getRuntime().removeShutdownHook(this);
        } catch (Exception e) {
            LOG.log(Level.INFO, "Unable to remove shutdown hook.", e);
        }
    }

    public void run() {
        if (etlThread != null && etlThread.isAlive() && !etlThread.isInterrupted()) {
            //cannot use logging in a shutdown hook
            System.out.println("Stopping ETL and rolling back changes...");
            try {
                etlThread.interrupt();
            } catch (Exception e) {
                System.err.println("Unable to interrupt ETL");
                e.printStackTrace();
            }

        }
    }
}
