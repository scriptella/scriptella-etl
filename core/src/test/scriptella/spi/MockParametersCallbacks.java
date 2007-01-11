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
package scriptella.spi;

import java.util.Map;

/**
 * Set of common implementations for testing.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class MockParametersCallbacks {
    public static final ParametersCallback SIMPLE = new ParametersCallback() {
        public Object getParameter(final String name) {
            return "*"+name+"*";
        }
    };

    public static final ParametersCallback NAME = new ParametersCallback() {
        public Object getParameter(final String name) {
            return name;
        }
    };


    public static final ParametersCallback NULL = new ParametersCallback() {
        public Object getParameter(final String name) {
            return null;
        }
    };

    public static final ParametersCallback UNSUPPORTED = new ParametersCallback() {
        public Object getParameter(final String name) {
            throw new UnsupportedOperationException("Parameter "+name);
        }
    };

    public static ParametersCallback fromMap(final Map map) {
        return new ParametersCallback() {
            public Object getParameter(final String name) {
                return map.get(name);
            }
        };
    }

}
