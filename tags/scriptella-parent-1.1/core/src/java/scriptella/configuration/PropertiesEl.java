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
package scriptella.configuration;

import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.ParametersCallback;
import scriptella.spi.support.MapParametersCallback;
import scriptella.util.IOUtils;
import scriptella.util.PropertiesMap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;


/**
 * Represents XML elements which store properties.
 * <p>Examples: <code>&lt;properties&gt;</code>
 * and <code>&lt;connection&gt;</code>
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class PropertiesEl extends XmlConfigurableBase {
    Map<String, ?> map;

    public PropertiesEl() {
    }

    public PropertiesEl(XmlElement element) {
        configure(element);
    }

    public void configure(final XmlElement element) {
        map = Collections.emptyMap();
        if (element == null) {
            return; //Properties is not a mandatory element
        }
        //optimization: if properties is empty - do not perform any additional steps
        if (element.getElement().hasChildNodes()) {
            PropertiesMap p = new PropertiesMap();
            ContentEl content = new ContentEl(element);
            InputStream is = null;

            try {
                //TODO use unicode conversion similar to native2ascii
                //expand global properties
                is = new ByteArrayInputStream(element.expandProperties(IOUtils.toString(content.open())).getBytes());
                p.load(is);
                //Now let's expand local properties
                PropertiesSubstitutor ps = new PropertiesSubstitutor(p);
                for (Map.Entry<String, Object> entry : p.entrySet()) {
                    Object v = entry.getValue();
                    if (v instanceof String) {
                        entry.setValue(ps.substitute((String) v));
                    }
                }
                map = p;
            } catch (Exception e) {
                throw new ConfigurationException("Unable to load properties", e,
                        element);
            } finally {
                IOUtils.closeSilently(is);
            }
        }
    }

    /**
     * Returns properties stored in this element as a map.
     * @return map of properties.
     */
    public Map<String, ?> getMap() {
        return map;
    }

    /**
     * Returns this properties as a parameters callback.
     * @return this properties as a parameters callback.
     */
    public ParametersCallback asParametersCallback() {
        return new MapParametersCallback(map);
    }

}
