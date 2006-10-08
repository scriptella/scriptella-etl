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
 * Progress indicator to send out
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConsoleProgressIndicator extends ProgressIndicatorBase {
    private static final Logger LOG = Logger.getLogger(ConsoleProgressIndicator.class.getName());
    private final DecimalFormat decimalFormat = new DecimalFormat("###");
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

        r.append(decimalFormat.format(100 * percentage));
        r.append('%');
        LOG.info(r.toString());
    }

    protected void onComplete(final String label) {
        LOG.info(((title == null) ? "" : title + ".") + "Complete");
    }
}
