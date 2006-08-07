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
package scriptella.interactive;

import java.text.DecimalFormat;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConsoleProgressIndicator extends ProgressIndicatorBase {
    private static final Logger LOG = Logger.getLogger(ConsoleProgressIndicator.class.getName());
    private static final DecimalFormat DF = new DecimalFormat("###");
    private String title;

    public ConsoleProgressIndicator(String title) {
        this.title = title;
    }

    public ConsoleProgressIndicator() {
    }

    protected void show(final String label, final double percentage) {
        StringBuilder r = new StringBuilder(32);

        if (title != null) {
            r.append(title);
        }

        if (label != null) {
            if (title != null) {
                r.append(".");
            }

            r.append(label);
        }

        r.append(": ");

        r.append(DF.format(100 * percentage));
        r.append('%');
        println(r);
    }

    protected void println(final Object o) {
        LOG.info(String.valueOf(o));
    }

    protected void onComplete(final String label) {
        println((title == null) ? "" : (title + " Complete"));
    }
}
