/*
 * Copyright 2006-2007 The Scriptella Project Team.
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
package scriptella.driver.velocity;

import scriptella.AbstractTestCase;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;

/**
 * Performance test for velocity connection provider.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class VelocityPerfTest extends AbstractTestCase {
    /**
     * This method tests velocity driver under load (5 threads * 5000 iterations).
     */
    public void test() throws InterruptedException {
        final CountDownLatch cdl = new CountDownLatch(5);
        for (int t=0;t<5;t++) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        VelocityConnection con = VelocityConnectionTest.createConnection(out);
                        for (int i=0;i<5000;i++) {
                            VelocityConnectionTest.run(con);
                            out.reset();
                        }
                        con.close();
                    } finally {
                        cdl.countDown();
                    }
                }
            }).start();
        }
        cdl.await();
    }
}
