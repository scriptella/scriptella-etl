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
package scriptella.configuration;

import scriptella.jdbc.GenericDriver;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.Resource;
import scriptella.spi.support.MapParametersCallback;
import scriptella.util.CollectionUtils;
import scriptella.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal bridge between execution and configuration packages for direct SQL.
 * Not a supported programmatic configuration API. Applications should use
 * {@link scriptella.execution.EtlExecutor#newSqlFileExecutor(File, String)}.
 */
public final class SqlFileConfigurationFactory {
    private SqlFileConfigurationFactory() {
    }

    /** Internal helper used by EtlExecutor's SQL factory. */
    public static ConfigurationEl create(final File file, String url, String user, String password,
                                         String driver, boolean substitution) {
        Map<String, Object> variables = new HashMap<String, Object>(CollectionUtils.asMap(System.getProperties()));
        PropertiesSubstitutor substitutor = new PropertiesSubstitutor(variables);
        url = substitutor.substitute(url);
        user = substitutor.substitute(user);
        password = substitutor.substitute(password);
        driver = substitutor.substitute(driver);
        if (file == null || url == null || !url.startsWith("jdbc:")) {
            throw new IllegalArgumentException("A SQL file and JDBC URL are required");
        }
        ConnectionEl connection = new ConnectionEl(
                url, user, password, Collections.singletonMap("substitution", substitution));
        connection.setDriver(driver == null ? GenericDriver.class.getName() : driver);
        connection.setLazyInit(true);
        ScriptEl script = new ScriptEl(new Resource() {
            public Reader open() throws IOException {
                return Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
            }

            @Override
            public String toString() {
                return file.toString();
            }
        });
        try {
            return new ConfigurationEl(IOUtils.toUrl(file), new MapParametersCallback(variables),
                    Collections.singletonList(connection), Collections.<ScriptingElement>singletonList(script));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Wrong SQL file path", e);
        }
    }
}
