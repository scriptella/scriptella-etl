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
package scriptella.core;

import java.io.IOException;

/**
 * Runtime wrapper for IO exceptions propagation.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class RuntimeIOException extends SystemException {

    public RuntimeIOException(IOException cause) {
        super(cause.getMessage(), cause);
    }

    public final Throwable initCause(Throwable cause) {
        throw new UnsupportedOperationException("Method not supported");
    }

    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
