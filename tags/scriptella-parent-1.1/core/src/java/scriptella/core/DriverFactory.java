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
package scriptella.core;

import scriptella.jdbc.GenericDriver;
import scriptella.spi.ScriptellaDriver;

import java.sql.Driver;

/**
 * Factory for Scriptella Service Providers and JDBC drivers.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public final class DriverFactory {
    //singleton
    private DriverFactory() {
    }

    /**
     * Loads a driver specified by a full or a short name.
     * This method tries to lookup a driver class specified by a name.
     * <p>The looking up procedure is the following:
     * <ol>
     * <li>Class with name <code>driverName</code> is looked up.
     * <li>If class not found, a class with the following name is checked:
     * scriptella.driver.&lt;driverName&gt;.Driver
     * <li>If no classes found - an exception is raised.
     * </ol>
     * <p>
     * @param driverName driver class short or full name.
     * @param loader class loader to use when loading driver classes.
     * @return found driver class.
     * @throws ClassNotFoundException if no drivers satisfy specified name.
     */
    public static ScriptellaDriver getDriver(String driverName, ClassLoader loader) throws ClassNotFoundException {
        //try direct match
        try {
            return getDriver(Class.forName(driverName, true, loader));
        } catch (ClassNotFoundException e) {
            //if not found try to produce a full name from a short one
            String fullName = "scriptella.driver."+driverName+".Driver";
            try {
                return getDriver(Class.forName(fullName, true, loader));
            } catch (ClassNotFoundException ignoredException) {
                throw e;//it's not a typo - return the first exception
            }

        }


    }

    /**
     * Creates a Scriptella Driver using specified class.
     * <p>If class is a {@link Driver} {@link GenericDriver JDBC Bridge} is used.
     * <p>To be successfully instantiated the driver class must implement {@link ScriptellaDriver} class
     * and has no-arg public constructor.
     *
     * @param drvClass driver class.
     * @return Scriptella Driver
     */
    @SuppressWarnings("unchecked")
    public static ScriptellaDriver getDriver(Class drvClass) {
        if (Driver.class.isAssignableFrom(drvClass)) {
            //We must load JDBC driver using the same classloader as drvClass
            try {

                ClassLoader drvClassLoader = drvClass.getClassLoader();
                if (drvClassLoader==null) { //if boot classloader is used,
                    // load scriptella driver using the class loader for this class.
                    drvClassLoader=DriverFactory.class.getClassLoader();
                }
                Class<?> cl = Class.forName("scriptella.jdbc.GenericDriver", true, drvClassLoader);
                return (ScriptellaDriver)cl.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to instantiate JDBC driver for " + drvClass, e);

            }
        } else if (ScriptellaDriver.class.isAssignableFrom(drvClass)) {
            try {
                return (ScriptellaDriver) drvClass.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to instantiate driver for " + drvClass, e);
            }

        } else {
            throw new IllegalArgumentException("Class " + drvClass + " is not a scriptella compatible driver.");
        }


    }
}
