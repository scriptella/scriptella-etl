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
package scriptella.driver.xpath;

import scriptella.spi.ProviderException;

/**
 * Thrown to indicate a problem with XML file queriing.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class XPathProviderException extends ProviderException {
    public XPathProviderException() {
    }

    public XPathProviderException(String message) {
        super(message);
    }

    public XPathProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public XPathProviderException(Throwable cause) {
        super(cause);
    }

    public String getProviderName() {
        return Driver.DIALECT.getName();
    }


}
