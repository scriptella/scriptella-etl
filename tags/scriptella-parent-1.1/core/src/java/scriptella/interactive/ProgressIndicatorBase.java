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
package scriptella.interactive;

/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class ProgressIndicatorBase implements ProgressIndicator {
    private double lastPercentage = 0;

    public void showProgress(final double progress, final String message) {
        if (progress == 1) {
            onComplete(message);

            return;
        }

        if ((progress - lastPercentage) > getOutputThreshold()) {
            show(message, progress);
            lastPercentage = progress;
        }
    }

    protected double getOutputThreshold() {
        return 0;
    }

    protected abstract void show(final String label, final double progress);

    protected void onComplete(final String label) {
        show(label, 1);
    }
}
