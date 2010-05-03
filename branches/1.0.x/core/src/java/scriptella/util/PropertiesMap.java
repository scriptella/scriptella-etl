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
package scriptella.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Represents a persistent set of properties.
 * <p>This class is a replacement for {@link Properties} class.
 * <p>Please note that {@link #put(String,Object)} has additional semantics.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class PropertiesMap implements Map<String, Object> {
    private Map<String, Object> props;

    public PropertiesMap() {
        props = new LinkedHashMap<String, Object>();
    }

    public PropertiesMap(int initialCapacity) {
        props = new LinkedHashMap<String, Object>(initialCapacity);
    }

    public PropertiesMap(Map<String, ?> props) {
        this(props.size());
        putAll(props);
    }

    /**
     * Creates a properties map from the stream.
     *
     * @param is stream to {@link #load(java.io.InputStream) load} properties from.
     * @throws IOException if I/O error occurs
     * @see #load(java.io.InputStream)
     */
    public PropertiesMap(InputStream is) throws IOException {
        this();
        load(is);
    }

    public int size() {
        return props.size();
    }

    public boolean isEmpty() {
        return props.isEmpty();
    }

    public boolean containsKey(Object key) {
        return props.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return props.containsValue(value);
    }

    public Object get(Object key) {
        return props.get(key);
    }

    /**
     * Put the property to underlying map.
     * <p>The properties are immutable, i.e. if the property is already present in the map, the new value is ignored.
     *
     * @param key   property name
     * @param value property value
     * @return value associated with specified key,
     *         or null if there was no mapping for key.
     */
    public Object put(String key, Object value) {
        Object old = props.get(key);
        if (old == null) {
            props.put(key, value);
        }
        return old;
    }

    public Object remove(Object key) {
        return props.remove(key);
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        for (Entry<? extends String, ? extends Object> entry : t.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        props.clear();
    }

    public Set<String> keySet() {
        return props.keySet();
    }

    public Collection<Object> values() {
        return props.values();
    }

    public Set<Entry<String, Object>> entrySet() {
        return props.entrySet();
    }

    public boolean equals(Object o) {
        return props.equals(o);
    }

    public int hashCode() {
        return props.hashCode();
    }

    /**
     * Loads properties using {@link Properties#load(java.io.InputStream)}.
     * <p>Properties order is preserved
     *
     * @param is input stream with properties.
     * @throws IOException if I/O error occurs.
     */
    public void load(InputStream is) throws IOException {
        new Properties() { //Overrides Properties to preserve insertion order

            public Object put(final Object k, final Object v) {
                return PropertiesMap.this.put((String) k, v);
            }
        }.load(is);
    }

    


    public String toString() {
        return String.valueOf(props);
    }
}
