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
package scriptella.driver.ldap.ldif;

import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.ParametersCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Buffered reader which overrides {@link #readLine()} to
 * allow substitution of context variables.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 * @see PropertiesSubstitutor
 */
public class SubstitutingLineReader extends BufferedReader {
    private final ParametersCallback callback;
    private final PropertiesSubstitutor substitutor = new PropertiesSubstitutor();
    private List<String> lines;
    private int len;


    /**
     * Creates instance of buffered reader.
     *
     * @param in       a reader to wrap.
     * @param callback callback to use for variables substitution.
     */
    public SubstitutingLineReader(final Reader in, final ParametersCallback callback) {
        super(in);
        this.callback = callback;
    }

    public String readLine() throws IOException {
        String s = super.readLine();
        if (s==null || s.length()==0) {
            return s;
        }
        s = substitutor.substitute(s, callback);
        if (lines!=null) { //if track lines
            lines.add(s); //remember the string
            len+=s.length()+1;//and increase the len (\n is included)
        }
        return s;
    }

    /**
     * @return read lines after calling the {@link #trackLines()}.
     */
    public String getTrackedLines() {
        if (lines==null) { //Check if tracking has been switched on.
            throw new IllegalStateException("Lines tracking must be switched on prior to calling this method");
        }
        StringBuilder sb = new StringBuilder(len);
        for (String s : lines) {
            sb.append(s).append('\n');
        }
        return sb.toString();
    }

    /**
     * Starts to track lines obtained by {@link #readLine()}.
     * <p>The previously tracked content is cleared.
     * @see #getTrackedLines()
     */
    public void trackLines() {
        if (lines!=null) {
            lines.clear();
        } else {
            lines=new ArrayList<String>();
        }
        len=0;
    }


}
