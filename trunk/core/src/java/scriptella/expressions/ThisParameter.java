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
/**
 * $Header: $
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2005
 * All rights reserved.
 */
package scriptella.expressions;

import scriptella.execution.ScriptsContext;

import java.net.MalformedURLException;
import java.net.URL;


/**
 * Goobal  variable available in all expressions.
 * <p>ThisParameter is obtained by using <code>this</code> variable name in
 * expressions, all public methods/properties may be invoked from expressions using
 * method invocation syntax.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ThisParameter {
    public static final String NAME = "this";
    private ScriptsContext ctx;

    public ThisParameter(ScriptsContext ctx) {
        this.ctx = ctx;
    }

    public URL getScriptFileURL() {
        return ctx.getBaseURL();
    }

    /**
     * @param fileUrl absolute URL or relative to script base URL.
     * @return FileParameter for corresponding file url.
     * @see scriptella.execution.ScriptsContext#getBaseURL()
     */
    public FileParameter file(final String fileUrl) {
        try {
            return new FileParameter(new URL(ctx.getBaseURL(), fileUrl));
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param callback callback to lookup <code>this</code> parameter.
     * @return <code>this</code> parameter.
     */
    public static ThisParameter get(final ParametersCallback callback) {
        return (ThisParameter) callback.getParameter(NAME);
    }
}
