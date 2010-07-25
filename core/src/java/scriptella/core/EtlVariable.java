/*
 * Copyright 2006-2009 The Scriptella Project Team.
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

import scriptella.spi.ParametersCallback;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Represents a global <code>etl</code> variable available for all ETL file elements.
 * <p>As of 1.1 a new syntax is introduced based on
 * <a href="http://commons.apache.org/jexl/reference/syntax.html#Functions">JEXL function</a> namespaces:
 * <ul>
 * <li><code>date:</code> namespace contains functions from
 * {@link EtlVariable.DateUtils}, e.g. <code>date:now()</code></li>
 * <li><code>text:</code> namespace contains functions from
 * {@link EtlVariable.TextUtils}, e.g. <code>text:ifNull()</code></li>
 * <li><code>class:</code> namespace contains functions from
 * {@link EtlVariable.ClassUtils},
 * e.g. <code>class:forName('java.lang.System').getProperty('java.version')</code></li>
 * </ul>
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlVariable implements ParametersCallback {


    public static final String NAME = "etl";


    private final DateUtils date = new DateUtils();
    private final TextUtils text = new TextUtils();
    private final ClassUtils clazz = new ClassUtils();
    private ParametersCallback parametersCallback;
    private Map<String, Object> globals;


    public EtlVariable(ParametersCallback parametersCallback, Map<String, Object> globals) {
        this.parametersCallback = parametersCallback;
        this.globals = globals;
    }

    public EtlVariable() {
    }

    public DateUtils getDate() {
        return date;
    }

    public TextUtils getText() {
        return text;
    }

    public ClassUtils getClazz() {
        return clazz;
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    /**
     * Accessor method for parameters available in the current execution context.
     * Useful for cases when variables has special symbols which cannot be referenced as normal JEXL variables, e.g.
     * <code>etl.getParameter('Column Name')</code>
     *
     * @param name parameter name.
     * @return value of the parameter.
     */
    public Object getParameter(String name) {
        return parametersCallback.getParameter(name);
    }

    /**
     * Getter for a global variables storage.
     * <p>This map is shared between all etl file elements and can be used for cases when there is a need to share some state globally.
     * It is recommended to avoid using global variables except cases when it's really beneficiary and simplifies the ETL file.
     * @return map of global variables in the scope of ETL file.
     */
    public Map<String, Object> getGlobals() {
        return globals;
    }

    /**
     * Utility class for ETL file expressions.
     * <p>Provides format/parse operation for date/time.
     */
    public static class DateUtils {
        /**
         * Returns a current date/time formatted according to the specified format.
         *
         * @param formatPattern format pattern as specified by {@link java.text.SimpleDateFormat}.
         * @return formatted string representation of current date/time.
         * @see java.text.SimpleDateFormat
         * @see #format(java.util.Date,String)
         */
        public String now(String formatPattern) {
            return format(now(), formatPattern);
        }

        /**
         * Formats a specified date/time according to the specified pattern.
         *
         * @param date          date to format.
         * @param formatPattern format pattern as specified by {@link java.text.SimpleDateFormat}.
         * @return formatted string representation of the specified date/time.
         */
        public String format(Date date, String formatPattern) {
            return new SimpleDateFormat(formatPattern).format(date);
        }

        public Date parse(String dateStr, String formatPattern) throws ParseException {
            return new SimpleDateFormat(formatPattern).parse(dateStr);
        }

        /**
         * Returns the current date.
         *
         * @return current date/time.
         */
        public Date now() {
            return new Date();
        }

        /**
         * Returns the formatted representation of current date.
         * <p>Time part is skipped.
         *
         * @return formatted representation of current date.
         */
        public String today() {
            return DateFormat.getDateInstance().format(now());
        }

        /**
         * A synonym for {@link #now(String)}.
         *
         * @param formatPattern format pattern.
         * @return formatted date.
         */
        public String today(String formatPattern) {
            return now(formatPattern);
        }
    }

    /**
     * Utility class for ETL file expressions.
     * <p>Provides text operations.
     * <p><u>Note:</u> As of version 1.1 the same functionality can be achieved by directly
     * using JEXL2.0 syntax elements like ternary operators.
     */
    public static class TextUtils {

        /**
         * Substitute an object when a null value is encountered.
         *
         * @param object      object to check.
         * @param replacement replacement object.
         * @return object or replacement if object==null
         */
        public Object ifNull(Object object, Object replacement) {
            return object == null ? replacement : object;
        }

        /**
         * Substitute an object with empty string when a null value is encountered.
         *
         * @param object object to check.
         * @return object or empty string if object==null.
         */
        public Object ifNull(Object object) {
            return object == null ? "" : object;
        }

        /**
         * If expr1 = expr2 is true, return NULL else return expr1.
         *
         * @param expr1 first expression
         * @param expr2 second expression
         * @return true if expr1 = expr2, otherwise null.
         */
        public Object nullIf(Object expr1, Object expr2) {
            if (expr1 == null) {
                return null;
            } else {
                return expr1.equals(expr2) ? null : expr1;
            }
        }

    }

    /**
     * Utility class for ETL expressions.
     * <p>Provides class utilities.
     */
    public static class ClassUtils {
        public Class forName(String className) throws ClassNotFoundException {
            return Class.forName(className);
        }
    }

}
