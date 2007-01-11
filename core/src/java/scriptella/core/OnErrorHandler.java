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

import scriptella.configuration.OnErrorEl;
import scriptella.configuration.ScriptEl;
import scriptella.spi.ProviderException;
import scriptella.util.ExceptionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Error handler for scripting elements.
 * <p>This class maintains an internal copy of {@link #onerrorElements} for
 * {@link ScriptEl} to allow calling {@link #onError(Throwable)} several times
 * with different return result.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class OnErrorHandler {
    private List<OnErrorEl> onerrorElements;
    //Stores error identifiers

    /**
     * Initialize error handler with the list of {@link OnErrorEl}.
     *
     * @param scriptEl scripting element configuration to get onerror info.
     */
    public OnErrorHandler(ScriptEl scriptEl) {
        List<OnErrorEl> onerrorElements = scriptEl.getOnerrorElements();
        if (onerrorElements != null && onerrorElements.size() > 0) {
            this.onerrorElements = new ArrayList<OnErrorEl>(onerrorElements);
        }
    }

    /**
     * Called on error to get a {@link OnErrorEl} with fallback script.
     * <p><em>Note:</em> The returned OnErrorEl element is removed from internal list of elements to avoid
     * handling the same error condition twice.
     *
     * @param throwable
     * @return OnErrorEl matching error condition.
     */
    public OnErrorEl onError(Throwable throwable) {
        if (onerrorElements == null) {
            return null;
        }
        //walks down the throwables chain
        for (Throwable t = throwable; t != null; t = ExceptionUtils.getCause(t)) {
            //iterates through elements
            for (OnErrorEl onErrorEl : onerrorElements) {
                Pattern type = onErrorEl.getType();
                if (type != null && !type.matcher(t.getClass().getName()).matches()) {
                    continue;
                }
                Pattern msg = onErrorEl.getMessage();
                //if onerror has message, but exception hasn't or different message - skip this case
                if (msg != null && (t.getMessage() == null || !msg.matcher(t.getMessage()).matches())) {
                    continue;
                }
                Set<String> codes = onErrorEl.getCodes();
                if (codes != null && !codes.isEmpty()) {
                    Set<String> errorCodes = getErrorCodes(t);
                    boolean match = false;
                    //check if onerror matches any error code for the throwable
                    for (String ec : errorCodes) {
                        if (codes.contains(ec)) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        continue;
                    }
                }
                //Next time we should not visit this element
                onerrorElements.remove(onErrorEl);
                return onErrorEl;
            }
        }
        return null;

    }

    /**
     * @param t throwables chain.
     * @return not null set of found error codes in throwable chain.
     */
    protected Set<String> getErrorCodes(Throwable t) {
        if (t instanceof ProviderException) {
            return ((ProviderException) t).getErrorCodes();
        }
        return Collections.emptySet();
    }


}
