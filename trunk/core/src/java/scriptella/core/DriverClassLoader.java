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
package scriptella.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * A classloader for drivers specified by connection element.
 * <p>This class loader is used only if classpath connection attribute is not empty.
 * @see #loadClass(String) delegation model description.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class DriverClassLoader extends URLClassLoader {
    public DriverClassLoader(URL[] urls) {
        super(urls, DriverClassLoader.class.getClassLoader());
    }

    /**
     * Loads a class specified by name.
     * <p>This class loader has a specific delegation model:
     * <ul>
     * <li>If a class package is a drivers package (jdbc or drivers), the
     * class is loaded and created using this classloader without delegating to the parent.
     * This solution is used to overcome limitations of cross-loaders interaction and to simplify built-in drivers development.
     * <li>In other cases the semantics is the same as in {@link URLClassLoader}.
     * </ul>
     * @param name class name.
     * @return the loaded class
     * @throws ClassNotFoundException If the class was not found
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name.startsWith("scriptella.jdbc.") || name.startsWith("scriptella.driver.")) {
            byte[] b = getClassBytes(name);
            if (b != null) {
                definePackage(name);
                return defineClass(name, b, 0, b.length);
            }
        }
        return super.loadClass(name);
    }

    /**
     * Defines a package for a class name.
     * @param className class name to define a package.
     * @see #definePackage(String, java.util.jar.Manifest, java.net.URL)
     */
    private void definePackage(String className) {
        if (className==null) {
            return;
        }
        int ind = className.lastIndexOf('.');
        if (ind<0) {
            return;
        }
        String pName = className.substring(0, ind);
        if (getPackage(pName)==null) {
            definePackage(pName, null ,null, null ,null, null, null, null);
        }
    }

    /**
     * Loads a class content using a parent class loader.
     * <p>Please note that we load class bytes even if it has already been loaded by the parent class loader.
     * @param name class name.
     * @return class file content.
     */
    private static byte[] getClassBytes(final String name) {
        String path = '/' + name.replace('.', '/') + ".class";
        InputStream is = DriverClassLoader.class.getResourceAsStream(path);
        if (is == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        byte[] b = new byte[1024];
        try {
            for (int c; (c = is.read(b)) >= 0;) {
                baos.write(b, 0, c);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return baos.toByteArray();

    }


}
