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

import scriptella.spi.DialectIdentifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Represents &lt;onerror&gt; xml element.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class OnErrorEl extends XmlConfigurableBase {
    private static final Pattern CODES_SEPARATOR = Pattern.compile("\\s*\\,\\s*");
    private Pattern type;
    private Pattern message;
    private Set<String> codes;
    private boolean retry;
    private String connectionId;
    protected DialectBasedContentEl content;

    public OnErrorEl() {
    }

    public OnErrorEl(final XmlElement element) {
        configure(element);
    }

    public void configure(final XmlElement element) {
        setPatternProperty(element, "type");
        setPatternProperty(element, "message");
        String codestr = element.getAttribute("codes");
        if (codestr == null) {
            codes = Collections.emptySet();
        } else {
            codes = new LinkedHashSet<String>(Arrays.asList(CODES_SEPARATOR.split(codestr)));
        }
        retry = element.getBooleanAttribute("retry", false);
        content = new DialectBasedContentEl(element);
        setProperty(element, "connection-id", "connectionId");
    }

    /**
     * @return Regular expression pattern to match exception type.
     */
    public Pattern getType() {
        return type;
    }

    public void setType(Pattern type) {
        this.type = type;
    }

    /**
     * @return Regular expression pattern to match exception message
     */
    public Pattern getMessage() {
        return message;
    }

    public void setMessage(Pattern message) {
        this.message = message;
    }

    /**
     * @return set of vendor codes/sql states.
     */
    public Set<String> getCodes() {
        return codes;
    }

    public void setCodes(Set<String> codes) {
        this.codes = codes;
    }

    /**
     * @return true if statement which caused a problem should be retried after onerror handler. Default value if false.
     */
    public boolean isRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }

    public ContentEl getContent(DialectIdentifier id) {
        return content.getContent(id);
    }

    public String getConnectionId() {
        return connectionId;
    }

    @SuppressWarnings("unused") //Called via reflection in configure
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String toString() {
        StringBuilder res = new StringBuilder("OnError{");
        if (type!=null) {
            res.append("type=").append(type).append(", ");
        }
        if (message!=null) {
            res.append("message=").append(message).append(", ");
        }
        if (codes!=null) {
            res.append("codes=").append(codes).append(", ");
        }

        res.append("retry=").append(retry).append('}');
        return res.toString();
    }
}
