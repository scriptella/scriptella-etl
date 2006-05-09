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
package scriptella.spi;

import scriptella.jdbc.ScriptellaJDBCDriver;

import java.sql.Driver;

/**
 * Factory for Scriptella Service Providers and JDBC drivers.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public final class DriversFactory {
    //singleton
    private DriversFactory() {
    }

    /**
     * Load a class for <code>driverClassName</code> and creates
     * a driver using {@link #getDriver(Class)}.
     * <p>Thread.currentThread().getContextClassLoader() is used to obtain class loader.
     *
     * @param driverClassName driver class name.
     * @return Scriptella Driver.
     * @throws ClassNotFoundException if If the class was not found.
     */
    public static ScriptellaDriver getDriver(String driverClassName) throws ClassNotFoundException {
        return getDriver(Class.forName(driverClassName));
    }

    /**
     * Creates a Scriptella Driver using specified class.
     * <p>If class is a {@link Driver} {@link ScriptellaJDBCDriver JDBC Bridge} is used.
     * <p>To be successfully instantiated the driver class must implement {@link ScriptellaDriver} class
     * and has no-arg public constructor.
     *
     * @param drvClass driver class.
     * @return Scriptella Driver
     */
    @SuppressWarnings("unchecked")
    public static ScriptellaDriver getDriver(Class drvClass) {
        if (Driver.class.isAssignableFrom(drvClass)) {
            //We don't have to pass driver class, because it must register itself
            return new ScriptellaJDBCDriver();
        } else if (ScriptellaDriver.class.isAssignableFrom(drvClass)) {
            try {
                return (ScriptellaDriver) drvClass.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to instantiate driver for class " + drvClass);
            }

        } else {
            throw new IllegalArgumentException("Class " + drvClass + " is not a scriptella compatible driver.");
        }


    }
}
