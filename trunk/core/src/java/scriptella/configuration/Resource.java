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
package scriptella.configuration;

import java.io.IOException;
import java.io.Reader;


/**
 * Represents reference to a text resource capable of creating new readers.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public interface Resource {
    /**
     * Opens a resource and returns a content reader.
     * <p>The returned reader implementation should be effective enough to allow usage without
     * extra buffering, etc.
     *
     * @return resource content reader
     * @throws IOException if I/O error occurs.
     */
    Reader open() throws IOException;
}
