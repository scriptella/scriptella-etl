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
package scriptella.sql;

/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DialectIdentifier {
    private String name;
    private String version;

    public DialectIdentifier(String name) {
        this.name = name;
    }

    public DialectIdentifier(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DialectIdentifier)) {
            return false;
        }

        final DialectIdentifier dialectIdentifier = (DialectIdentifier) o;

        if ((name != null) ? (!name.equals(dialectIdentifier.name))
                : (dialectIdentifier.name != null)) {
            return false;
        }

        if ((version != null) ? (!version.equals(dialectIdentifier.version))
                : (dialectIdentifier.version != null)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = ((name != null) ? name.hashCode() : 0);
        result = (29 * result) + ((version != null) ? version.hashCode() : 0);

        return result;
    }

    public String toString() {
        return "DialectIdentifier{" + "name='" + name + "'" + ", version='" +
                version + "'" + "}";
    }
}
