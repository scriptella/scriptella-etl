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
package scriptella.spi;

/**
 * Optional interface implemented by {@link Connection} classes. It is introduced as a separate interface for
 * preserving backwards compatibility.
 * <p>
 * Represents a Scriptella object which is able to return the native connection used to talk with the
 * datasource. It is up to the driver to decide which object is returned.
 * For example Scriptella adapters for JDBC drivers return an instance of
 * {@link java.sql.Connection}.
 * 
 * 
 * 
 * @author Fyodor Kupolov
 * @version 1.2
 * @see Connection
 */
public interface NativeConnectionProvider {
	/**
	 * Returns the native connection which is wrapped by this object or null if this information is not
	 * available.
	 * 
	 * @return native connection which is wrapped by this object.
	 */
	public Object getNativeConnection();
}
