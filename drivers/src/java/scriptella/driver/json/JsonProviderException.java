/*
 * Copyright 2026 The Scriptella Project Team.
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
package scriptella.driver.json;

import scriptella.spi.ProviderException;

/** JSON configuration, selection, framing or I/O failure. */
public class JsonProviderException extends ProviderException {
    public JsonProviderException(String message) {
        super(message);
    }

    public JsonProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getProviderName() {
        return "JSON";
    }
}
