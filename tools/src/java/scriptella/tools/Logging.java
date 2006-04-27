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
package scriptella.tools;

import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.*;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class Logging {
    /**
     * Configures logging messages to be printed to console
     * todo Add another method for outputting to swing application
     */
    public static void configure() {
        final Logger l = Logger.getLogger("scriptella");
        l.setLevel(Level.INFO);
        l.setUseParentHandlers(false);

        final ConsoleHandler h = new ConsoleHandler();
        final MessageFormat f = new MessageFormat("{0,date} {0,time} <{1}> {2}");
        final StringBuffer sb = new StringBuffer();
        final Date d = new Date();
        final Object args[] = new Object[3];
        args[0] = d;
        h.setFormatter(new Formatter() {
            public synchronized String format(final LogRecord record) {
                d.setTime(record.getMillis());
                args[1] = record.getLevel().getLocalizedName();
                args[2] = record.getMessage();

                f.format(args, sb, null).toString();
                final Throwable err = record.getThrown();
                sb.append('\n');
                if (err!=null) {
                    sb.append(err.getMessage());
                    sb.append('\n');
                }
                final String s = sb.toString();
                sb.setLength(0);

                return s;
            }
        });
        l.addHandler(h);
    }


}
