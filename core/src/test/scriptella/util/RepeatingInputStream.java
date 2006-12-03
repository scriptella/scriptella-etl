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
package scriptella.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream that repeats a source content.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class RepeatingInputStream extends InputStream {
    private int count;
    private byte data[];
    private int pos;

    public RepeatingInputStream(byte data[], int count) {
        this.data = data;
        this.count = count;
    }

    public int read() throws IOException {
        if (count<=0 || pos >= data.length) {
            count--;

            if (count > 0) {
                pos = 0;
            } else {
                return -1;
            }
        }

        int r = data[pos] & 0xff;
        pos++;

        return r;
    }

}
