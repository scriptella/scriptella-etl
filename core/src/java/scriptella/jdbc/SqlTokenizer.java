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
package scriptella.jdbc;

import java.io.Closeable;
import java.io.IOException;

/**
 * This interface provides a contract to iterate SQL statements.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public interface SqlTokenizer extends Closeable {
    int[] EMPTY_INJECTIONS_ARRAY = new int[0];
    /**
     * Parses the following SQL statement from the source.
     * <p>Use {@link #getInjections()} to obtain recognized injections, e.g.
     * binding variables and/or expressions.
     *
     * @return parsed SQL statement or null if EOF.
     * @throws IOException if I/O exception occurs
     */
    String nextStatement() throws IOException;

    /**
     * This method returns list of injections for the last returned statement.
     * @return injections for the last returned statement.
     */
    int[] getInjections();
}
