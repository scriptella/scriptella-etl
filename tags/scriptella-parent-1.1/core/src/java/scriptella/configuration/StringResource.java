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
package scriptella.configuration;

import scriptella.spi.Resource;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Represents String as a resource.
 * <p>This implementation is intended to represent
 * small text blocks.
 * <p>This resource is immutable and can be cached based
 * on object identity.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class StringResource implements Resource {
    private final String string;
    private final String description;

    /**
     * Creates a resource based on String content.
     *
     * @param string resource content.
     */
    public StringResource(String string) {
        this(string, null);
    }

    /**
     * Creates a resource based on String content.
     *
     * @param string      resource content.
     * @param description resource description.
     */
    public StringResource(String string, String description) {
        this.string = string;
        this.description = description;
    }

    public Reader open() throws IOException {
        return new StringReader(string);
    }


    /**
     * Returns the string wrapped by this resource.
     */
    public String getString() {
        return string;
    }

    public String toString() {
        return description == null ? "Text block" : description;
    }
}
