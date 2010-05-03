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
package scriptella.interactive;

import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * Scriptella runtime configurer for java.util.Logging.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class LoggingConfigurer {
    private LoggingConfigurer() {
    }

    /**
     * Configures logging messages to use specified handler
     * @param handler to use.
     */
    public static void configure(Handler handler) {
        final Logger l = getScriptellaLogger();
        l.setLevel(handler.getLevel());
        l.setUseParentHandlers(false);
        l.addHandler(handler);
    }

    public static void remove(Handler handler) {
        getScriptellaLogger().removeHandler(handler);
    }

    private static Logger getScriptellaLogger() {
        return Logger.getLogger("scriptella");
    }


}
