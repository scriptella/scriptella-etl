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
package scriptella.driver.script;

import scriptella.expression.LineIterator;
import scriptella.spi.ProviderException;
import scriptella.spi.Resource;
import scriptella.util.ExceptionUtils;
import scriptella.util.IOUtils;

import javax.script.ScriptException;
import java.io.IOException;

/**
 * Thrown to indicate an error in scripting engine.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptProviderException extends ProviderException {
    public ScriptProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScriptProviderException(String message, Resource resource, ScriptException exception) {
        super(message, exception);
        int lines = exception.getLineNumber() - 1;
        LineIterator it = null;
        try {
            it = new LineIterator(resource.open());
            if (it.skip(lines) == lines && it.hasNext()) {
                setErrorStatement(it.next());
            }
        } catch (IOException e) {
            ExceptionUtils.ignoreThrowable(e);
        } finally {
            IOUtils.closeSilently(it);
        }
    }


    public String getProviderName() {
        return "javax.script";
    }
}
