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

import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.ParametersCallback;
import scriptella.util.CollectionUtils;
import scriptella.util.PropertiesMap;

import java.util.Map;
import java.util.Properties;

/**
 * A merger class for external and local xml properties.
 * <p>This class may be used as a {@link ParametersCallback}
 * in properties substitution. Use {@link #getSubstitutor()} for substitution.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class PropertiesMerger implements ParametersCallback {
    private PropertiesMap properties;
    private PropertiesSubstitutor substitutor=new PropertiesSubstitutor(this);

    public PropertiesMerger() {
        this.properties = new PropertiesMap();
    }

    public PropertiesMerger(Properties properties) {
        this.properties = new PropertiesMap(CollectionUtils.asMap(properties));
    }

    public PropertiesMerger(Map<String,String> properties) {
        this.properties = new PropertiesMap(properties);
    }

    /**
     * Adds properties and expands their values by evaluating expressions and property references.
     *
     * @param properties properties to add to scripts context.
     * @see #getSubstitutor()
     */
    void addProperties(final Map<String, String> properties) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            this.properties.put(entry.getKey(), substitutor.substitute(entry.getValue()));
        }
    }

    public Object getParameter(final String name) {
        return properties.get(name);
    }

    /**
     * Returns a property substitutor which is using properties of this merger.
     * <p>The later changes to this class properties are automatically visible to the returned substitutor.
     * @return properties substitutor based on this merger properties.
     */
    public PropertiesSubstitutor getSubstitutor() {
        return substitutor;
    }


    public String toString() {
        return "PropertiesMerger{" +
                properties +
                '}';
    }
}
