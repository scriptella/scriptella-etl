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

import scriptella.util.PropertiesMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class PropertiesEl extends XMLConfigurableBase {
    Map<String, String> map = Collections.emptyMap();

    public PropertiesEl() {
    }

    public PropertiesEl(XMLElement element) {
        configure(element);
    }

    public void configure(final XMLElement element) {
        if (element == null) {
            return; //Properties is not a mandatory element
        }
        //optimization: if properties is empty - do not perform any additional steps
        if (element.getElement().hasChildNodes()) {
            PropertiesMap p = new PropertiesMap();
            ContentEl content = new ContentEl(element);
            InputStream is = null;

            try {
                is = new ReaderInputStream(content.open());
                p.load(is);
                map = p;
            } catch (Exception e) {
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
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = new PropertiesMap(map);
    }
}
