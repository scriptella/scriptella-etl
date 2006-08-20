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

import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.ParametersCallback;

/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class LdapPropertiesSubstitutor extends PropertiesSubstitutor {
    private final ParametersCallback callback;

    public LdapPropertiesSubstitutor(final ParametersCallback callback) {
        this.callback = callback;
    }

    public String substitute(final String s) {
        return super.substitute(s, callback);
    }

    protected String toString(final Object o) {
        final String s = super.toString(o);
        if (s == null || s.length() == 0) {
            return s;
        }
        //The following check is used to obey the contract of readLine method
        //the returned value must not have line separators
        if (s.indexOf('\n') >= 0) {
            throw new IllegalStateException("Multi line content variables substitution is not supported");
        }
        return s;
    }
}
