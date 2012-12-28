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

package scriptella;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Logging configurer which keeps messages sent to a specified logger.
 *
 * @author Fyodor Kupolov
 */
public class TestLoggingConfigurer {
    public static final int MAX_LOG_MESSAGEBUFFER_SIZE = 100000; //100 KB
    private String loggerName;
    private boolean oldUseParent;
    private TestHandler handler;

    public TestLoggingConfigurer(String loggerName) {
        this.loggerName = loggerName;
    }

    public void setUp() {
        final Logger logger = Logger.getLogger(loggerName);
        oldUseParent = logger.getUseParentHandlers();
        logger.setUseParentHandlers(true);
        handler = new TestHandler();
        logger.addHandler(handler);
    }

    public void tearDown() {
        if (handler != null) {
            final Logger logger = Logger.getLogger(loggerName);
            logger.setUseParentHandlers(oldUseParent);
            logger.removeHandler(handler);
            handler = null;
        }
    }

    public int getMessageCount(String msg) {
        int count = 0;
        for (int nextIndex = 0; nextIndex >= 0; ) {
            nextIndex = handler.buf.indexOf(msg, nextIndex);
            if (nextIndex >= 0) {
                nextIndex += msg.length();
                count++;
            }
        }
        return count;
    }

    private static class TestHandler extends Handler {
        StringBuilder buf = new StringBuilder();

        @Override
        public void publish(LogRecord record) {
            buf.append(record.getLoggerName()).append('|').append(record.getLevel()).append('|').append(record.getMessage()).append('\n');
            if (buf.length() > MAX_LOG_MESSAGEBUFFER_SIZE) {
                buf.delete(0, buf.length() / 2);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }
}
