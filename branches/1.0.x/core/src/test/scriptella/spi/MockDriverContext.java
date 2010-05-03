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
package scriptella.spi;

import scriptella.util.IOUtils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Mock implementation of drivers context suitable for typical cases.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class MockDriverContext implements DriverContext {
    public URL getScriptFileURL() {
        try {
            return new URL("file:/mock");
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public URL resolve(final String uri) throws MalformedURLException {
        return IOUtils.resolve(getScriptFileURL(),uri);
    }

    public static final DriverContext INSTANCE = new MockDriverContext();


    public Object getParameter(final String name) {
        return null;
    }
}
