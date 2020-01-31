/*
 * Copyright 2006-2020 The Scriptella Project Team.
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
package scriptella.driver.shell;

import scriptella.util.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runner for shell commands, abstracting threading and process details.
 */
public class ShellCommandRunner {
    private final ExecutorService execService;
    private String[] fullArgs;
    private final BufferedWriter out;
    private Process proc;
    protected BufferedReader procOutputReader;
    protected BufferedReader procErrReader;
    private AtomicReference<Throwable> readInputError;
    private AtomicReference<Throwable> readErrError;
    private CountDownLatch finishedProcessingStreamsSignal;

    public ShellCommandRunner(final String[] shellCmdArgs, final BufferedWriter out) {
        fullArgs = Arrays.copyOf(shellCmdArgs, shellCmdArgs.length + 1);
        this.out = out;
        execService = Executors.newFixedThreadPool(2);
    }

    public void exec(String cmdText) throws IOException {
        // Change the actual command as a last arg
        fullArgs[fullArgs.length - 1] = cmdText;
        execAndInitReaders(fullArgs);
        readInputError = new AtomicReference<>();
        readErrError = new AtomicReference<>();
        finishedProcessingStreamsSignal = new CountDownLatch(2);
        execService.submit(() -> {
            String s;
            try {
                while ((s = procOutputReader.readLine()) != null) {
                    out.write(s);
                    out.newLine();
                }
                out.flush();
                procOutputReader.close();
            } catch (Throwable throwable) {
                readInputError.set(throwable);
            }
            finishedProcessingStreamsSignal.countDown();
        });
        execService.submit(() -> {
            String s;
            try {
                while ((s = procErrReader.readLine()) != null) {
                    System.err.println(s);
                }
                procErrReader.close();
            } catch (Throwable throwable) {
                readErrError.set(throwable);
            }
            finishedProcessingStreamsSignal.countDown();
        });
    }

    /**
     * Wait for the process to finish (including stdout/stderr finished forwarding) and check for errors during processing its output
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws ExecutionException if an error occurred during processing output of the process (srdin/stderr)
     */
    public void waitForAndCheckExceptions() throws InterruptedException, ExecutionException {
        waitForProc();
        // Even though the process is done, we may still be processing buffered output from it
        finishedProcessingStreamsSignal.await();
        if (readInputError.get() != null) {
            throw new ExecutionException("An error occurred while processing stdout of the process", readInputError.get());
        }
        if (readErrError.get() != null) {
            throw new ExecutionException("An error occurred while processing stderr of the process", readErrError.get());
        }
    }

    public void executeAfterStdoutStderrConsumed(Runnable runnable) {
        execService.submit(() -> {
            try {
                finishedProcessingStreamsSignal.await();
                runnable.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void close() {
        execService.shutdownNow();
        IOUtils.closeSilently(procErrReader);
        IOUtils.closeSilently(procOutputReader);
        if (proc != null) {
            proc.destroy();
            proc = null;
        }
    }

    /**
     * Can be subclassed for testing and no use Process at all.
     */
    protected void execAndInitReaders(String[] args) throws IOException {
        proc = Runtime.getRuntime().exec(args);
        procOutputReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        procErrReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
    }

    protected void waitForProc() throws InterruptedException {
        proc.waitFor();
    }

}
