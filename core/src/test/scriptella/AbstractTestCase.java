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
package scriptella;

import junit.framework.TestCase;
import scriptella.configuration.ConfigurationEl;
import scriptella.configuration.ConfigurationFactory;
import scriptella.execution.ScriptsExecutor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public abstract class AbstractTestCase extends TestCase {
    protected static final String RESOURCES_DIR_NAME = "resources";
    protected static TestURLHandler testURLHandler;

    static {
        //Registering tst URL, subclasses should set testUrlStreamHandler in test method if they use "tst" url
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
            public URLStreamHandler createURLStreamHandler(final String protocol) {
                if ("tst".equals(protocol)) {
                    return new URLStreamHandler() {
                        protected URLConnection openConnection(final URL u) {
                            return new URLConnection(u) {
                                public void connect() {
                                }

                                public InputStream getInputStream()
                                        throws IOException {
                                    return testURLHandler.getInputStream(u);
                                }

                                public int getContentLength() {
                                    return testURLHandler.getContentLength(u);
                                }
                            };
                        }
                    };
                }

                return null;
            }
        });
    }

    protected final File resourceBaseDir;

    public AbstractTestCase() {
        setName(getClass().getName());
        //If running under maven/ant - we use basedir
        String projectBaseDir = System.getProperty("basedir");

        if (projectBaseDir == null) {
            projectBaseDir = "core";
        }
        resourceBaseDir = new File(projectBaseDir, "src/test/" + RESOURCES_DIR_NAME);
    }


    /**
     * This method returns file with path relative to project's src/test/resources directory
     *
     * @param path file path relative to project's src/test/resources directory
     * @return file with path relative to project's src/test/resources directory
     */
    protected File getFileResource(final String path) {
        return new File(resourceBaseDir, path);
    }

    protected ScriptsExecutor newScriptsExecutor() {
        String name = this.getClass().getName();
        final String p = AbstractTestCase.class.getPackage().getName() + '.';
        int ind = name.indexOf(p);

        if (ind >= 0) { //if test class name starts with scriptella package - we remove it
            name = name.substring(p.length());
        }

        name = name.replace('.', '/'); //make path
        name += ".xml";

        return newScriptsExecutor(name);
    }

    protected ScriptsExecutor newScriptsExecutor(final String path) {
        return newScriptsExecutor(loadConfiguration(path));
    }

    protected ScriptsExecutor newScriptsExecutor(final ConfigurationEl configuration) {
        return new ScriptsExecutor(configuration);
    }

    protected ConfigurationEl loadConfiguration(final String path) {
        String newPath = path;
        if (!path.startsWith("/")) {
            newPath = RESOURCES_DIR_NAME + '/' + path;
        } else {
            newPath = path.substring(1);
        }

        ConfigurationFactory cf = new ConfigurationFactory();
        final URL resource = Thread.currentThread().getContextClassLoader()
                .getResource(newPath);

        if (resource == null) {
            throw new IllegalStateException("Resource " + newPath + " not found");
        }

        cf.setResourceURL(resource);

        return cf.createConfiguration();
    }

    protected static interface TestURLHandler {
        public InputStream getInputStream(final URL u)
                throws IOException;

        public int getContentLength(final URL u);
    }
}
