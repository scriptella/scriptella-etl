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
package scriptella.jdbc;

import scriptella.spi.ConnectionParameters;

import java.lang.reflect.Constructor;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Utility class JDBC related operations.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public final class JdbcUtils {
    private static final Pattern URL_PROPERTY = Pattern.compile("(?:[?;&])([^=?;&]+)=([^;&]*)");
    private static final Pattern URL_USER_INFO = Pattern.compile("^jdbc:[^:]+://([^/@]+)@");

    private JdbcUtils() {
    }

    /**
     * Returns a copy of a JDBC exception with connection details removed from
     * its messages. The complete {@link SQLException#getNextException()} chain
     * is copied as well.
     *
     * @param source     exception to sanitize
     * @param parameters connection parameters
     * @param properties effective properties passed to the JDBC driver
     * @return sanitized exception
     */
    static SQLException sanitize(SQLException source, ConnectionParameters parameters,
                                 Properties properties) {
        SQLException sanitized = copy(source, parameters, properties);
        SQLException tail = sanitized;
        for (SQLException next = source.getNextException(); next != null; next = next.getNextException()) {
            SQLException sanitizedNext = copy(next, parameters, properties);
            tail.setNextException(sanitizedNext);
            tail = sanitizedNext;
        }
        return sanitized;
    }

    private static SQLException copy(SQLException source, ConnectionParameters parameters, Properties properties) {
        String message = sanitizeMessage(source.getMessage(), parameters, properties);
        SQLException copy = newSQLException(source, message,
                copyCause(source.getCause(), parameters, properties));
        copy.setStackTrace(source.getStackTrace());
        return copy;
    }

    private static SQLException newSQLException(SQLException source, String message, Throwable cause) {
        try {
            Constructor<? extends SQLException> constructor = source.getClass().getConstructor(
                    String.class, String.class, Integer.TYPE, Throwable.class);
            return constructor.newInstance(message, source.getSQLState(), source.getErrorCode(), cause);
        } catch (ReflectiveOperationException | SecurityException e) {
            return new SQLException(message, source.getSQLState(), source.getErrorCode(), cause);
        }
    }

    private static Throwable copyCause(Throwable source, ConnectionParameters parameters, Properties properties) {
        if (source == null) {
            return null;
        }
        if (source instanceof SQLException) {
            return sanitize((SQLException) source, parameters, properties);
        }
        String message = sanitizeMessage(source.getMessage(), parameters, properties);
        Throwable copy = newThrowable(source, message,
                copyCause(source.getCause(), parameters, properties));
        copy.setStackTrace(source.getStackTrace());
        return copy;
    }

    private static Throwable newThrowable(Throwable source, String message, Throwable cause) {
        try {
            Constructor<? extends Throwable> constructor = source.getClass().getConstructor(
                    String.class, Throwable.class);
            return constructor.newInstance(message, cause);
        } catch (NoSuchMethodException e) {
            return newThrowableWithMessage(source, message, cause);
        } catch (ReflectiveOperationException e) {
            return fallbackThrowable(source, message, cause);
        } catch (SecurityException e) {
            return fallbackThrowable(source, message, cause);
        }
    }

    private static Throwable newThrowableWithMessage(Throwable source, String message, Throwable cause) {
        try {
            Constructor<? extends Throwable> constructor = source.getClass().getConstructor(String.class);
            Throwable copy = constructor.newInstance(message);
            copy.initCause(cause);
            return copy;
        } catch (ReflectiveOperationException | SecurityException | IllegalStateException e) {
            return fallbackThrowable(source, message, cause);
        }
    }

    private static Throwable fallbackThrowable(Throwable source, String message, Throwable cause) {
        return new Exception(source.getClass().getName() +
                (message == null ? "" : ": " + message), cause);
    }

    private static String sanitizeMessage(String message, ConnectionParameters parameters, Properties properties) {
        message = sanitize(message, parameters.getUrl(), getUrlDescription(parameters.getUrl()));
        message = sanitize(message, parameters.getPassword(), "[hidden]");
        for (String sensitiveValue : getSensitiveUrlValues(parameters.getUrl())) {
            message = sanitize(message, sensitiveValue, "[hidden]");
        }
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (isSensitiveProperty(String.valueOf(entry.getKey()))) {
                message = sanitize(message, String.valueOf(entry.getValue()), "[hidden]");
            }
        }
        return message;
    }

    private static Set<String> getSensitiveUrlValues(String url) {
        Set<String> values = new LinkedHashSet<String>();
        if (url == null) {
            return values;
        }
        Matcher properties = URL_PROPERTY.matcher(url);
        while (properties.find()) {
            if (isSensitiveProperty(properties.group(1))) {
                addUrlValue(values, properties.group(2));
            }
        }
        Matcher userInfo = URL_USER_INFO.matcher(url);
        if (userInfo.find()) {
            String value = userInfo.group(1);
            int passwordSeparator = value.indexOf(':');
            if (passwordSeparator >= 0) {
                addUrlValue(values, value.substring(passwordSeparator + 1));
            }
        }
        return values;
    }

    private static void addUrlValue(Set<String> values, String value) {
        if (value.length() > 0) {
            values.add(value);
            try {
                values.add(URLDecoder.decode(value, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                // Keep the raw value when URL encoding is malformed.
            }
        }
    }

    static String getUrlDescription(String url) {
        if (url != null && url.startsWith("jdbc:")) {
            int separator = url.indexOf(':', 5);
            if (separator > 5) {
                return "JDBC URL scheme " + url.substring(0, separator + 1);
            }
        }
        return "the configured JDBC URL";
    }

    private static String sanitize(String message, String sensitiveValue, String replacement) {
        if (message == null || sensitiveValue == null || sensitiveValue.length() == 0) {
            return message;
        }
        return message.replace(sensitiveValue, replacement);
    }

    private static boolean isSensitiveProperty(String name) {
        String normalized = name.toLowerCase(Locale.ENGLISH);
        return normalized.contains("password") || normalized.contains("passwd") ||
                normalized.contains("secret") || normalized.contains("token") ||
                normalized.contains("credential");
    }

    /**
     * Silently closes a connection.
     * @param con connection to close. Nulls allowed.
     */
    public static void closeSilent(final Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        } catch (SQLException e) {
        }
    }

    /**
     * Silently closes a statement.
     * @param s statement to close. Nulls allowed.
     */
    public static void closeSilent(final Statement s) {
        try {
            if (s != null) {
                s.close();
            }
        } catch (SQLException e) {
        }
    }

    /**
     * Silently closes a result set.
     * @param rs result set to close. Nulls allowed.
     */
    public static void closeSilent(final ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
        }
    }

}
