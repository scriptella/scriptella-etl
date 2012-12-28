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
package scriptella.driver.lucene;

import scriptella.spi.ProviderException;

/**
 * Thrown to indicate Lucene failure.
 */
public class LuceneProviderException extends ProviderException {
    public LuceneProviderException() {
    }

    public LuceneProviderException(String message) {
        super(message);
    }

    public LuceneProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public LuceneProviderException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getProviderName() {
        return Driver.DIALECT_IDENTIFIER.getName();
    }

    @Override
    public ProviderException setErrorStatement(String errStmt) {
        return super.setErrorStatement(errStmt);
    }

}
