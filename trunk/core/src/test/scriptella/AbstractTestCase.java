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
import scriptella.spi.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
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
    protected static TestURLHandler testURLHandler;
    protected static final File resourceBaseDir;

    static {
        //If running under maven/ant - we use basedir
        String projectBaseDir = System.getProperty("basedir");

        if (projectBaseDir == null) {
            projectBaseDir = "core";
        }
        resourceBaseDir = new File(projectBaseDir, "src/test");
        
        //Registering tst URL, subclasses should set testUrlStreamHandler in test method if they use "tst" url
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
            public URLStreamHandler createURLStreamHandler(final String protocol) {
                if ("tst".equals(protocol)) {
                    return new URLStreamHandler() {
                        protected URLConnection openConnection(final URL u) {
                            return new URLConnection(u) {
                                public void connect() {
                                }

                                public OutputStream getOutputStream() throws IOException {
                                    return testURLHandler.getOutputStream(u);
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

    public AbstractTestCase() {
        setName(getClass().getSimpleName());
    }


    /**
     * This method returns file with path relative to project's src/test directory
     *
     * @param path file path relative to project's src/test directory
     * @return file with path relative to project's src/test directory
     */
    protected File getFileResource(final String path) {
        return new File(resourceBaseDir, path);
    }

    protected ScriptsExecutor newScriptsExecutor() {
        String name = getClass().getSimpleName()+".xml";
        return newScriptsExecutor(name);
    }

    protected ScriptsExecutor newScriptsExecutor(final String path) {
        return newScriptsExecutor(loadConfiguration(path));
    }

    protected ScriptsExecutor newScriptsExecutor(final ConfigurationEl configuration) {
        return new ScriptsExecutor(configuration);
    }

    protected ConfigurationEl loadConfiguration(final String path) {
        ConfigurationFactory cf = new ConfigurationFactory();
        final URL resource = getClass().getResource(path);

        if (resource == null) {
            throw new IllegalStateException("Resource " + path + " not found");
        }

        cf.setResourceURL(resource);

        return cf.createConfiguration();
    }

    /**
     * Removes extra whitespace characters.
     *
     * @param s string to replace.
     * @return s with extra whitespace chars removed.
     */
    protected String removeWhitespaceChars(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    protected static interface TestURLHandler {
        public InputStream getInputStream(final URL u)
                throws IOException;
        public OutputStream getOutputStream(final URL u)
                throws IOException;

        public int getContentLength(final URL u);
    }

    /**
     * Converts specified resource to String.
     * @param content content to convert.
     * @return resource content as String.
     */
    protected static String asString(final Resource content) {
        char cb[] = new char[4096];
        StringBuilder sb = new StringBuilder(8192);

        try {
            Reader reader = content.open();

            for (int c = 0; (c = reader.read(cb)) > 0;) {
                sb.append(cb, 0, c);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return sb.toString();
    }

}
