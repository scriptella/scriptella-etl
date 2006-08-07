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

import javax.swing.*;
import java.awt.*;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SwingProgressIndicator extends ProgressIndicatorBase {
    private ProgressWindow w;

    public SwingProgressIndicator(String title) {
        w = new ProgressWindow(title);
    }

    protected void show(final String label, final double progress) {
        if (!w.isVisible()) {
            w.setVisible(true);
        }

        w.update(label, (int) (progress * 100));

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
        }
    }

    protected void onComplete(final String label) {
        super.onComplete(label);
        w.setVisible(false);
        w.dispose();
    }

    private static class ProgressWindow extends JFrame {
        JProgressBar pb = new JProgressBar(0, 100);

        public ProgressWindow(String title) throws HeadlessException {
            super(title);
            pb.setStringPainted(true);
            pb.setPreferredSize(new Dimension(500, 20));
            getContentPane().add(pb);
            pack();
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            setLocationRelativeTo(null);
        }

        public void update(final String message, final int progress) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    pb.setValue(progress);
                    pb.setString(message);
                }
            });
        }
    }
}
