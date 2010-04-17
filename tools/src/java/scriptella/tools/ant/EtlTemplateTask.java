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
package scriptella.tools.ant;

import org.apache.tools.ant.BuildException;
import scriptella.tools.template.TemplateManager;

import java.io.IOException;
import java.util.Map;

/**
 * Task to emit ETL file templates.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlTemplateTask extends EtlTaskBase {
    private String name;

    /**
     * Setter for name property.
     *
     * @param name template name.
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void execute() throws BuildException {
        if (name == null || name.trim().length() == 0) {
            throw new BuildException("The name attribute is required");
        }
        try {
            setupLogging();
            create(TemplateManager.forName(name), getProperties());
        } catch (Exception e) {
            throw new BuildException("Unable to create template " + name + ". Reason: " + e, e);
        } finally {
            resetLogging();
        }
    }

    /**
     * Template method for testing purposes.
     *
     * @param tm         template manager.
     * @param properties properties.
     * @throws IOException if I/O error occurs.
     */
    protected void create(TemplateManager tm, Map<String, ?> properties) throws IOException {
        tm.create(properties);
    }
}
