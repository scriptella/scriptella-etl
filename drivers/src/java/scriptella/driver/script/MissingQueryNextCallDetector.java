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

package scriptella.driver.script;

import scriptella.core.RuntimeIOException;
import scriptella.spi.Resource;
import scriptella.util.IOUtils;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * CRQ-12257.
 * Helper class to detect and warn about missing query.next() calls.
 *
 * @author Fyodor Kupolov
 * @version 1.1
 * @see scriptella.driver.script.ParametersCallbackMap#isNextCalled()
 */
public class MissingQueryNextCallDetector {
    private static boolean DISABLE_CHECKER = Boolean.getBoolean("scriptella.disable_missing_query_next_check");
    private static final Logger LOG = Logger.getLogger(MissingQueryNextCallDetector.class.getName());
    private static final Pattern QUERY_NEXT_CALL = Pattern.compile("query.*next", Pattern.DOTALL | Pattern.MULTILINE);
    private final ParametersCallbackMap map;
    private final Resource resource;

    public MissingQueryNextCallDetector(ParametersCallbackMap map, Resource resource) {
        this.map = map;
        this.resource = resource;
    }

    /**
     * Detects if a query.next() call is missing by using the following rules:
     * <ul>
     * <li>query.isNextCalled return false, i.e. it was never called</li>
     * <li>Heuristically checks that the source code of the script contains query.next(), by using the regex {@link #QUERY_NEXT_CALL}.
     * </ul>
     * @return true if missing query.next() was detected
     */
    public boolean detectMissingQueryNextCall() {
        if (DISABLE_CHECKER) {
            return false;
        }
        if (!map.isNextCalled()) {
            try {
                final String code = IOUtils.toString(resource.open());
                if (!QUERY_NEXT_CALL.matcher(code).find()) {
                    LOG.warning("query.next() was never called in query " + resource + ". Nested elements will not be executed. See querying example at http://goo.gl/LrOZS");
                    return true;
                }
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        }
        return false;
    }
}
