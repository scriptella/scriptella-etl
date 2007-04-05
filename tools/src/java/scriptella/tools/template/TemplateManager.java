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
package scriptella.tools.template;

import scriptella.core.SystemException;
import scriptella.util.IOUtils;
import scriptella.util.PropertiesMap;
import scriptella.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

/**
 * ETL files template manager.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class TemplateManager {
    private static final String DEFAULT_ETL_XML = "default.etl.xml";
    private static final String DEFAULT_ETL_PROPS = "default.etl.properties";
    private static final String DEFAULT_BASE_NAME = "etl";
    private static final String XML_EXT = ".xml";
    private static final String PROPS_EXT = ".properties";
    private static final String PACKAGE_NAME = TemplateManager.class.getName().substring(0,
            TemplateManager.class.getName().lastIndexOf('.'));
    static final TemplateManager DEFAULT = new TemplateManager();



    /**
     * Produce template files.
     *
     * @param properties configuration properties.
     * @throws IOException if output fails.
     */
    public void create(Map<String, ?> properties) throws IOException {
        //Only default template supported yet
        String baseName = defineName();
        String xmlName = baseName + XML_EXT;
        String propsName = baseName + PROPS_EXT;

        Writer w = newFileWriter(xmlName);

        w.write(MessageFormat.format(loadResourceAsString(DEFAULT_ETL_XML), propsName));
        w.close();
        w = newFileWriter(propsName);
        w.write(loadResourceAsString(DEFAULT_ETL_PROPS));
        w.close();
        System.out.println("Files " + xmlName + ", " + propsName + " have been successfully created.");
    }

    /**
     * Loads specifed classpath resource into a string.
     *
     * @param resourcePath path to resource.
     * @return loaded resource.
     * @throws IOException if IO error occurs.
     */
    protected String loadResourceAsString(String resourcePath) throws IOException {
        InputStream xml = getClass().getResourceAsStream(resourcePath);
        if (xml == null) {
            throw new FileNotFoundException("Resource " + resourcePath + " not found");
        }
        return IOUtils.toString(new InputStreamReader(xml));
    }


    /**
     * Defines base name for ETL.
     */
    protected String defineName() {
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

    /**
     * Creates an ETL template using a specified template manager name and properties file.
     *
     * @param name           etl template name.
     * @param propertiesFile configuration properties file.
     * @throws IOException if I/O error occurs.
     */
    public static void create(final String name, final String propertiesFile) throws IOException {
        TemplateManager template = DEFAULT;
        Map<String, ?> map = Collections.emptyMap();
        if (!StringUtils.isEmpty(name)) {
            template = forName(name);
            final File filePath = new File(StringUtils.isEmpty(propertiesFile) ? name + ".properties" : propertiesFile);
            if (filePath.isFile()) {
                map = new PropertiesMap(new FileInputStream(filePath));
            } else if (!StringUtils.isEmpty(propertiesFile)) { //If file was specified but absent - throw an exception
                throw new FileNotFoundException("File " + filePath.toString() + " not found");
            }
        }
        template.create(map);
    }


    /**
     * Loads a specified by name.
     *
     * @param name template name. Cannot be null.
     * @return loaded template manager.
     */
    static TemplateManager forName(final String name) {
        String className = PACKAGE_NAME + '.' + name;
        try {
            Class cl = Class.forName(className);
            return (TemplateManager) cl.newInstance();
        } catch (ClassNotFoundException e) {
            throw new SystemException("Template " + name + " not found", e);
        } catch (Exception e) {
            throw new SystemException("Cannot initialize template " + name, e);
        }
    }


}
