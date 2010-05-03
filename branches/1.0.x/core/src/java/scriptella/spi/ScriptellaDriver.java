/*
 * Copyright 2006-2009 The Scriptella Project Team.
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
package scriptella.spi;

/**
 * Service Provider Interface for integrating third-party systems with Scriptella.
 * Implementing class must have public no-args constructor.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public interface ScriptellaDriver {
    /**
     * Implementor should create a new connection based on specified parameters.
     *
     * @param connectionParameters connection parameters defined in &lt;connection&gt; element.
     * @return new connection.
     */
    Connection connect(ConnectionParameters connectionParameters);


    /**
     * @return Driver's meaningful name
     */
    String toString();
}
