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
package scriptella.tools.ant;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;


/**
 * Not in use now
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class ClasspathSupportTask extends Task {
    private Path classpath;
    private AntClassLoader classLoader;

    /**
     * Sets the classpath for loading the driver.
     *
     * @param classpath The classpath to set
     */
    public void setClasspath(final Path classpath) {
        this.classpath = classpath;
    }

    /**
     * Add a path to the classpath for loading the driver.
     */
    public Path createClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }

        return this.classpath.createPath();
    }

    /**
     * Set the classpath for loading the driver
     * using the classpath reference.
     */
    public void setClasspathRef(final Reference r) {
        createClasspath().setRefid(r);
    }

    public ClassLoader getClassLoader() {
        if (classpath != null) {
            if (classLoader == null) {
                classLoader = getProject().createClassLoader(classpath);
            }

            return classLoader;
        } else {
            return getProject().getCoreLoader();
        }
    }

    /**
     * Gets the classpath.
     *
     * @return Returns a Path
     */
    public Path getClasspath() {
        return classpath;
    }

}
