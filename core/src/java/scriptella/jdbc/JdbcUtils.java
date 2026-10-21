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
import java.util.IdentityHashMap;
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

    private static final Pattern USER_CONTEXT = Pattern.compile(
            "(?i)(\\b(?:user(?:name)?|login)(?:\\s*[=:]\\s*|\\s+))" +
                    "(\"[^\"]*\"|'[^']*'|[^\\s,;:&)\\]}\"']+?(?=[.!?]*(?:$|[\\s,;:&)\\]}])))");

    private JdbcUtils() {
    }

    /**
     * Returns a credential-safe copy of a JDBC exception.
     * <p>The SQL exception subtype, SQL metadata, ordinary cause chain, and
     * complete {@link SQLException#getNextException()} chain are retained when
     * their constructors permit it. Connection details are removed from all
     * copied messages.
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
        String diagnostic = source.getClass().getName() +
                (message == null ? "" : ": " + message);
        return source instanceof java.io.IOException
                ? new java.io.IOException(diagnostic, cause) : new Exception(diagnostic, cause);
    }

    private static String sanitizeMessage(String message, ConnectionParameters parameters, Properties properties) {
        message = sanitize(message, parameters.getUrl(), getUrlDescription(parameters.getUrl()));
        message = sanitize(message, parameters.getPassword(), "[hidden]");
        message = sanitizeUser(message, parameters.getUser());
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

    /**
     * Returns a credential-safe description of a JDBC URL.
     *
     * @param url JDBC URL, possibly null
     * @return the JDBC scheme or a generic configured-URL description
     */
    static String getUrlDescription(String url) {
        if (url != null && url.startsWith("jdbc:")) {
            int separator = url.indexOf(':', 5);
            if (separator > 5) {
                return "JDBC URL scheme " + url.substring(0, separator + 1);
            }
        }
        return "the configured JDBC URL";
    }

    /**
     * Returns a credential-safe description of an input/output URL.
     * Only the protocol is included; authority, path, query and fragment are omitted.
     *
     * @param url resolved URL, or null for console I/O
     * @return a source description without connection details
     */
    public static String getUrlDescription(java.net.URL url) {
        return url == null ? "console" : "URL scheme " + url.getProtocol() + ":";
    }

    /**
     * Copies an exception graph with source URL credentials removed from messages.
     * Causes, suppressed exceptions, stack traces and SQL diagnostics are retained;
     * repeated/cyclic references never attach an original exception to the copy.
     *
     * @param source exception to report
     * @param url source URL, or null for console I/O
     * @return sanitized diagnostic copy
     */
    public static Throwable sanitize(Throwable source, java.net.URL url) {
        Set<String> values = new LinkedHashSet<String>();
        if (url != null) {
            values.add(url.toExternalForm());
            String userInfo = url.getUserInfo();
            if (userInfo != null) {
                addUrlValue(values, userInfo);
                for (String part : userInfo.split(":", 2)) {
                    addUrlValue(values, part);
                }
            }
            // Redact every query value: credential parameter names are not standardized.
            String query = url.getQuery();
            if (query != null) {
                addUrlValue(values, query);
                for (String parameter : query.split("&")) {
                    int separator = parameter.indexOf('=');
                    addUrlValue(values, parameter.substring(separator + 1));
                }
            }
            if (url.getRef() != null) {
                addUrlValue(values, url.getRef());
            }
        }
        return copyUrlException(source, values, new IdentityHashMap<Throwable, Throwable>());
    }

    private static Throwable copyUrlException(Throwable source, Set<String> values,
                                               Map<Throwable, Throwable> copies) {
        if (source == null) {
            return null;
        }
        if (copies.containsKey(source)) {
            return copies.get(source);
        }
        // Break cause cycles before descending; the placeholder contains no source data.
        copies.put(source, new Exception("Repeated exception reference"));
        String message = source.getMessage();
        for (String value : values) {
            message = sanitize(message, value, "[hidden]");
        }
        Throwable cause = copyUrlException(source.getCause(), values, copies);
        Throwable copy = source instanceof SQLException
                ? newSQLException((SQLException) source, message, cause)
                : newThrowable(source, message, cause);
        copy.setStackTrace(source.getStackTrace());
        copies.put(source, copy);
        for (Throwable suppressed : source.getSuppressed()) {
            Throwable sanitized = copyUrlException(suppressed, values, copies);
            if (sanitized != copy) {
                copy.addSuppressed(sanitized);
            }
        }
        if (source instanceof SQLException) {
            SQLException next = ((SQLException) source).getNextException();
            if (next != null && !copies.containsKey(next)) {
                ((SQLException) copy).setNextException((SQLException) copyUrlException(next, values, copies));
            }
        }
        return copy;
    }

    private static String sanitizeUser(String message, String user) {
        if (message == null || user == null || user.isEmpty()) {
            return message;
        }
        // Only redact a complete value following a recognizable user/login label.
        // Short usernames must not replace characters inside unrelated diagnostics.
        Matcher context = USER_CONTEXT.matcher(message);
        StringBuffer result = new StringBuffer();
        while (context.find()) {
            String value = context.group(2);
            boolean quoted = value.startsWith("\"") || value.startsWith("'");
            String name = quoted ? value.substring(1, value.length() - 1) : value;
            if (user.equalsIgnoreCase(name)) {
                String replacement = quoted ? value.charAt(0) + "[hidden]" + value.charAt(0) : "[hidden]";
                context.appendReplacement(result, Matcher.quoteReplacement(context.group(1) + replacement));
            } else {
                context.appendReplacement(result, Matcher.quoteReplacement(context.group()));
            }
        }
        context.appendTail(result);
        return result.toString();
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
