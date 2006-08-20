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
package scriptella.driver.ldap;

import scriptella.spi.ProviderException;

/**
 * Thrown by LDAP Provider to indicate a failure.
 */
public class LdapProviderException extends ProviderException {
    public LdapProviderException() {
    }

    public LdapProviderException(String message) {
        super(message);
    }

    public LdapProviderException(String message, String errorStatement) {
        super(message);
        setErrorStatement(errorStatement);
    }

    public LdapProviderException(String message, String errorStatement, Throwable cause) {
        super(message, cause);
        setErrorStatement(errorStatement);
    }

    public LdapProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public LdapProviderException(Throwable cause) {
        super(cause);
    }

    public String getProviderName() {
        return Driver.DIALECT.getName();
    }

    public ProviderException setErrorStatement(String errStmt) {
        return super.setErrorStatement(errStmt);
    }

}
