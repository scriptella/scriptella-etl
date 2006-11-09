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
package scriptella.tools.template;

import scriptella.util.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Map;

/**
 * ETL files template manager.
 * <p>TODO Add support for DB migration script templates
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class TemplateManager {
    private static final String DEFAULT_ETL_XML = "default.etl.xml";
    private static final String DEFAULT_ETL_PROPS = "default.etl.properties";
    private Map<String, String> properties;
    private static final String DEFAULT_BASE_NAME = "etl";
    private static final String XML_EXT = ".xml";
    private static final String PROPS_EXT = ".properties";

    public TemplateManager() {
    }

    /**
     * Creates a template manager using configuration properties.
     */
    public TemplateManager(Map<String, String> properties) {
        this.properties = properties;
    }


    /**
     * Produce template files.
     *
     * @throws IOException if output fails.
     */
    public void create() throws IOException {
        //Only default template supported yet
        InputStream xml = getClass().getResourceAsStream(DEFAULT_ETL_XML);
        if (xml == null) {
            throw new IllegalArgumentException("Resource " + DEFAULT_ETL_XML + " not found");
        }
        InputStream props = getClass().getResourceAsStream(DEFAULT_ETL_PROPS);
        if (props == null) {
            throw new IllegalArgumentException("Resource " + DEFAULT_ETL_PROPS + " not found");
        }

        String baseName = defineName();
        String xmlName = baseName + XML_EXT;
        String propsName = baseName + PROPS_EXT;
        String xmlTemplate = IOUtils.toString(new InputStreamReader(xml));
        Writer w = newFileWriter(xmlName);

        w.write(MessageFormat.format(xmlTemplate, propsName));
        w.close();
        w = newFileWriter(propsName);
        String propsTemplate = IOUtils.toString(new InputStreamReader(props));
        w.write(propsTemplate);
        w.close();
        System.out.println("Files " + xmlName + ", " + propsName + " have been successfully created.");
    }

    /**
     * Defines base name for ETL.
     */
    String defineName() {
        for (int i = 0; i < 10; i++) {
            String name = DEFAULT_BASE_NAME + ((i > 0) ? ("[" + i + "]") : "");
            if (checkFile(name + XML_EXT) && checkFile(name + PROPS_EXT)) {
                return name;
            }
        }
        throw new IllegalStateException("Too many templates generated. Remove unused.");
    }

    /**
     * Template factory method for writers.
     */
    protected Writer newFileWriter(String fileName) throws IOException {
        return new FileWriter(fileName);
    }

    /**
     * Returns true if file doesn't exist.
     *
     * @param name file name.
     */
    protected boolean checkFile(String name) {
        File f = new File(name);
        return !f.exists();
    }
}
