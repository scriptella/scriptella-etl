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
package scriptella.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class PropertiesEl extends XMLConfigurableBase implements Map<String, String> {
    Map<String, String> properties = Collections.emptyMap();

    public PropertiesEl() {
    }

    public PropertiesEl(XMLElement element) {
        configure(element);
    }

    public void configure(final XMLElement element) {
        if (element == null) {
            return; //Properties is not a mandatory element
        }

        properties = new LinkedHashMap<String, String>();

        Properties props = new Properties() { //Overrides Properties to preserve insertion order

            public Object put(final Object k, final Object v) {
                if (properties.containsKey(k)) {
                    properties.remove(k); //the added property becomes last in the list.
                }

                return properties.put((String) k, (String) v);
            }
        };

        ContentEl content = new ContentEl(element);
        InputStream is = null;

        try {
            is = new ReaderInputStream(content.open());
            props.load(is);
        } catch (Exception e) {
            properties.clear(); //clear phantom properties - not to leave object in a partly constructed state
            throw new ConfigurationException("Unable to load properties", e,
                    element);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public int size() {
        return properties.size();
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    public boolean containsKey(final Object key) {
        return properties.containsKey(key);
    }

    public boolean containsValue(final Object value) {
        return properties.containsValue(value);
    }

    public String get(final Object key) {
        return properties.get(key);
    }

    public String put(final String key, final String value) {
        return properties.put(key, value);
    }

    public String remove(final Object key) {
        return properties.remove(key);
    }

    public void putAll(final Map<?extends String, ?extends String> t) {
        properties.putAll(t);
    }

    public void clear() {
        properties.clear();
    }

    public Set<String> keySet() {
        return properties.keySet();
    }

    public Collection<String> values() {
        return properties.values();
    }

    public Set<Entry<String, String>> entrySet() {
        return properties.entrySet();
    }

    public boolean equals(final Object o) {
        return properties.equals(o);
    }

    public int hashCode() {
        return properties.hashCode();
    }
}
