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

/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ProgressCallback {
    protected int size;
    protected int pos;
    private int reserved;
    protected ProgressIndicator progressIndicator;

    public ProgressCallback(int size) {
        this(size, null);
    }

    public ProgressCallback(int size, ProgressIndicator indicator) {
        this.size = size;
        progressIndicator = indicator;
    }

    public int step(final int step) {
        return step(step, null);
    }

    public int step(final int step, final String message) {
        if (pos < size) {
            int s = ((pos + step + reserved) > size) ? (size - pos - reserved)
                    : step;
            pos += s;
            showProgress(message);

            return s;
        }

        return 0;
    }

    public boolean isComplete() {
        return pos >= size;
    }

    public void complete() {
        if (pos < size) {
            step(size - pos);
        }
    }

    void freeReserved(final int step, final String message) {
        if (step > reserved) {
            int p = reserved;
            reserved = 0;
            step(p, message);
        } else {
            reserved -= step;
            step(step, message);
        }
    }

    public int getSize() {
        return size;
    }

    public int getPos() {
        return pos;
    }

    public void setProgressBar(final ProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    public ProgressCallback fork(final int newSize) {
        return fork(size - reserved - pos, newSize);
    }

    public ProgressCallback fork(final int step, final int newSize) {
        int s = ((pos + reserved + step) > size) ? (size - reserved - pos) : step;
        reserved += s;

        return new Subprogress(this, newSize, s);
    }

    private void showProgress(final String message) {
        if (progressIndicator != null) {
            progressIndicator.showProgress(((double) pos) / ((double) size),
                    message);
        }
    }

    public int getLeft() {
        int l = size - pos - reserved;

        return (l < 0) ? 0 : l;
    }

    protected static class Subprogress extends ProgressCallback {
        private ProgressCallback parent;
        private int oldSize;
        private int oldSizeLeft;
        private int accum;

        public Subprogress(ProgressCallback parent, int size, int oldSize) {
            super(size);
            this.parent = parent;
            this.oldSize = oldSize;
            oldSizeLeft = oldSize;
            accum = 0;
        }

        public int step(final int step, final String message) {
            if (pos < size) {
                int s = super.step(step, message);
                accum += s;

                final int oldStep = (pos == size) ? oldSizeLeft
                        : ((accum * oldSize) / size);

                if (oldStep > 0) {
                    parent.freeReserved(oldStep, message);
                    oldSizeLeft -= oldStep;
                    accum = 0;
                }

                return s;
            }

            return 0;
        }
    }
}
