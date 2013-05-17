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

import scriptella.execution.EtlContext;
import scriptella.spi.Connection;
import scriptella.spi.ParametersCallback;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Represents a global <code>etl</code> variable available for all ETL file elements.
 * <p>The variable provides the following functionalities:</p>
 * <ul>
 *     <li>Utility methods useful in JEXL expressions, e.g {@link DateUtils#format(java.util.Date, String)} or
 *     {@link TextUtils#nullIf(Object, Object)}</li>
 *     <li>{@link #getConnection(String)} method for obtaining underlying connection objects</li>
 *     <li>{@link #getGlobals()} - container for global variables shared across different parts of ETL file</li>
 * </ul>
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
 *<h2>Examples</h2>
 * <h3>Global variables</h3>
 * <p>A global variable, which is set in one part of the ETL file can be later read from other places during ETL execution:
 * <pre><code>
 *     &lt;query connection-id="db"&gt;
 *       SELECT COUNT(*) as userCount from Users
 *       &lt;script connection-id="jexl"&gt;
 *           etl.globals['globalVar'] = userCount;
 *       &lt;/script&gt;
 *     &lt;/query&gt;
 *     &lt;script connection-id="log"&gt;
 *         Global variable 'globalVar': ${etl.globals['globalVar']}
 *    &lt;/script&gt;
 * </code></pre>
 * <h3>Obtain a native connection to the JDBC datasource</h3>
 * <pre><code>
 *    &lt;script connection-id="java"&gt;
 *        scriptella.core.EtlVariable etl = (scriptella.core.EtlVariable)get("etl");
 *        java.sql.Connection c = (java.sql.Connection)((scriptella.spi.NativeConnectionProvider)etl.getConnection("db")).getNativeConnection();
 *        java.sql.ResultSet r = c.createStatement().executeQuery("SELECT COUNT(*) FROM Test");
 *        r.next();
 *        Object cnt = r.getObject(1);
 *        java.util.logging.Logger.getLogger("mylogger").info("Count: "+cnt);
 *    &lt;/script&gt;
 * </code></pre>
 *
 * @author Fyodor Kupolov
 * @since 1.0
 */
public class EtlVariable implements ParametersCallback {


    public static final String NAME = "etl";


    private final DateUtils date = new DateUtils();
    private final TextUtils text = new TextUtils();
    private final ClassUtils clazz = new ClassUtils();
    private ParametersCallback parametersCallback;
    private EtlContext globalContext;


    public EtlVariable(ParametersCallback parametersCallback, EtlContext globalContext) {
        this.parametersCallback = parametersCallback;
        this.globalContext = globalContext;
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
     * @since 1.1
     */
    public Map<String, Object> getGlobals() {
        return globalContext.getGlobalVariables();
    }

    /**
     * Returns the <code>{@link Connection connection}</code> for the specified id,
     * This method is convenient for cases when access to the native connection is required, e.g. invoking a specific method
     * or for a manual control over a transaction boundaries.
     * <p><b>Note:</b> The method should be used with caution because it might affect the flow of the ETL file execution
     * and block some optimizations which may be added to the ETL engine in the future.
     *
     * @param id id of the required connection. Null is allowed if script has only one connection.
     * @return connection for the specified id.
     * @since 1.2
     */
    public Connection getConnection(String id) {
        return globalContext.getSession().getConnection(id).getConnection();
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
