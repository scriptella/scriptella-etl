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
package scriptella.tools.template;

import scriptella.AbstractTestCase;
import scriptella.execution.EtlExecutorException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Integration test for {@link scriptella.tools.template.DataMigrator}.
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DataMigratorITest extends AbstractTestCase {
    private StringWriter etlFile=new StringWriter();
    private StringWriter etlPropsFile=new StringWriter();
    private static final String[] EXPECTED_TABLES = {"DEPARTMENTS", "PERSONS", "EMPLOYEES", "EMPLOYEES_DEPARTMENTS"};
    private static final Pattern INSERT_INTO_PTR = Pattern.compile("INSERT INTO (\\w+)");

    /**
     * Tests if data migrator correctly produces a template for HSQLDB table.
     */
    public void test() throws IOException, EtlExecutorException {
        DataMigrator dm = new DataMigrator() {

            protected Writer newFileWriter(String fileName) throws IOException {
                if (fileName.endsWith(".xml")) {
                    return etlFile;
                }
                return etlPropsFile;
            }

            protected boolean checkFile(String name) {
                return true;
            }
        };
        newEtlExecutor().execute();
        Map<String,String> props = new HashMap<String, String>();
        props.put(DataMigrator.DRIVER_PROPERTY_NAME, "org.hsqldb.jdbcDriver");
        props.put(DataMigrator.URL_PROPERTY_NAME, "jdbc:hsqldb:mem:dataMigratorTest");
        props.put(DataMigrator.USER_PROPERTY_NAME, "sa");
        dm.create(props);

        Matcher matcher = INSERT_INTO_PTR.matcher(etlFile.toString());
        for (String table : EXPECTED_TABLES) { //Extracting table names
            assertTrue(matcher.find());
            assertEquals(table, matcher.group(1)); //Comparing with expected list
        }
        assertFalse(matcher.find());
        assertTrue(etlPropsFile.toString().startsWith("#")); //Checks if properties file starts with comments
    }
}
