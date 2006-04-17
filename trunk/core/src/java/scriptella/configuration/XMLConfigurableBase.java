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

import scriptella.execution.SystemException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * Base class for configuration elements.
 */
public abstract class XMLConfigurableBase implements XMLConfigurable {
    protected void setRequiredProperty(final XMLElement element,
                                       final String attribute, final String property) {
        setProperty(element, attribute, property);
        assertRequiredFieldPresent(element, attribute, property);
    }

    protected void setProperty(final XMLElement element,
                               final String attribute, final String property) {
        String attributeValue = element.getAttribute(attribute);

        try {
            final Method method = findSetter(property);
            method.invoke(this, attributeValue);
        } catch (Exception e) {
            throw new ConfigurationException("Unable to set property " +
                    property + " from attribute " + attribute, e, element);
        }
    }

    protected Method findSetter(final String property)
            throws NoSuchMethodException {
        return getClass()
                .getMethod("set" +
                        Character.toUpperCase(property.charAt(0)) + property.substring(1),
                        new Class[]{String.class});
    }

    protected Method findGetter(final String property)
            throws NoSuchMethodException {
        return getClass()
                .getMethod("get" +
                        Character.toUpperCase(property.charAt(0)) + property.substring(1),
                        (Class[]) null);
    }

    protected void setProperty(final XMLElement element, final String attribute) {
        setProperty(element, attribute, attribute);
    }

    protected void setRequiredProperty(final XMLElement element,
                                       final String attribute) {
        setRequiredProperty(element, attribute, attribute);
    }

    protected <T> T loadClass(final XMLElement element, final String attribute,
                              final Class<T> spec) {
        final String h = element.getAttribute(attribute);

        if (h == null) {
            return null;
        }

        Class c = null;

        try {
            c = Class.forName(h);
        } catch (ClassNotFoundException e) {
            throw new scriptella.configuration.ConfigurationException(
                    "Invalid class " + h, e, element);
        }

        if (!spec.isAssignableFrom(c)) {
            throw new scriptella.configuration.ConfigurationException("Class " +
                    c + " doesn't implement " + spec, element);
        }

        try {
            return (T) c.newInstance();
        } catch (Exception e) {
            throw new scriptella.configuration.ConfigurationException(
                    "Unable to instantiate class " + c, element);
        }
    }

    protected <T extends XMLConfigurable> List<T> load(
            final List<XMLElement> elements, final Class<T> clazz) {
        if (elements == null) {
            return null;
        }

        List<T> l = new ArrayList<T>(elements.size());

        for (XMLElement element : elements) {
            try {
                T t = clazz.newInstance();
                t.configure(element);
                l.add(t);
            } catch (InstantiationException e) {
                throw new SystemException(e);
            } catch (IllegalAccessException e) {
                throw new SystemException(e);
            }
        }

        return l;
    }

    protected void assertRequiredFieldPresent(final XMLElement element,
                                              final String attribute, final String property) {
        try {
            final Method getter = findGetter(property);

            if (getter.invoke(this, (Object[]) null) == null) {
                throw new RequiredAttributeException(attribute, element);
            }
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new RequiredAttributeException(attribute, e, element);
        }
    }

    protected void assertRequiredFieldPresent(final XMLElement element,
                                              final String property) {
        assertRequiredFieldPresent(element, property, property);
    }
}
